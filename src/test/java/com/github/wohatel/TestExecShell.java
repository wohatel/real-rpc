package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.BashSession;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.util.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 *
 * @author yaochuang 2025/09/09 15:06
 */
public class TestExecShell {

    private static RpcServer server;
    private static RpcDefaultClient client;
    private static SessionManager<BashSession> sessionManager;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        RpcEventLoopManager eventLoopManager = RpcEventLoopManager.of(new NioEventLoopGroup());
        server = new RpcServer(8765, eventLoopManager);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765, eventLoopManager);
        // 等待客户端连接成功
        client.connect().sync();
        // 设置为1000秒,到期后自动关闭session
        sessionManager = new SessionManager<>(1000_000, (sessionId, session) -> {
            session.close();
        });
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendSessionMsg() throws InterruptedException {
        // 绑定服务端接收消息处理

        RpcSessionRequestMsgHandler serverSessionHandler = new RpcSessionRequestMsgHandler() {

            @Override
            public boolean sessionStart(ChannelHandlerContext ctx, RpcSessionContextWrapper contextWrapper) {
                RpcSessionContext context = contextWrapper.getRpcSessionContext();
                RpcSession rpcSession = contextWrapper.getRpcSession();
                System.out.println("此次会话主题是:" + context.getTopic());
                if (true) {// 构建shell
                    BashSession bashSession = new BashSession();
                    bashSession.onPrintOut(str -> {
                        // 此处以response的方式返回,接收方需要以future.addListener 方式监听
                        // 也可以用request的方式返回,但是另外一端需要以处理请求的方式
                        RpcReaction reaction = rpcSession.toReaction();
                        reaction.setBody(str);
                        RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                    });
                    sessionManager.initSession(rpcSession.getSessionId(), bashSession);
                } else {
                    // 服务端判断不满足条件,直接关闭
                    return false;
                }
                return true;
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSessionContextWrapper contextWrapper, RpcSessionRequest request) {
                // 假如客户端把命令写到body字段
                String command = request.getBody();
                BashSession session = sessionManager.getSession(contextWrapper.getRpcSession().getSessionId());
                // 将command也放入输出
                session.sendCommand(command);
                session.getOutputQueue().offer(command);
            }

            @Override
            public void sessionStop(ChannelHandlerContext ctx, RpcSessionContextWrapper contextWrapper) {
                System.out.println("关闭session");
                // 释放session资源--(release后,内部的在53行里面有个consumer,已经做了关闭,所以不顾要跟再做BashSession.close)
                sessionManager.release(contextWrapper.getRpcSession().getSessionId());
            }
        };

        server.onSessionRequestReceive(serverSessionHandler);

        // 这次会话最多-一旦30s内没有人说话,就算是被中断了
        RpcSession session = new RpcSession(30_000);
        // 请求开启会谈
        RpcSessionContext rpcSessionContext = new RpcSessionContext();
        rpcSessionContext.setTopic("开启shell");
        RpcSessionFuture rpcSessionFuture = client.startSession(session, rpcSessionContext);
        if (rpcSessionFuture.get().isSuccess()) {
            System.out.println("服务端已开启session" + rpcSessionFuture.get().getOrigin());
        }
        // 此处接收response的数据
        rpcSessionFuture.addListener(reaction -> {
            if (reaction.isSuccess()) {
                String body = reaction.getBody();
                System.out.println(body);
            }
        });

//         打印工作目录下的文件列表
        client.sendSessionRequest(new RpcSessionRequest(session, "ls -al"));
        Thread.sleep(1000);
//         切换了目录
        client.sendSessionRequest(new RpcSessionRequest(session, "cd /tmp"));
        // 打印/tmp下的文件目录
        client.sendSessionRequest(new RpcSessionRequest(session, "ls -al"));

        // 防止线程退出
        Thread.currentThread().join();
    }
}
