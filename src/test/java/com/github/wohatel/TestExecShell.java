package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.BashSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionReactionWaiter;
import com.github.wohatel.interaction.file.RpcSessionSignature;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.util.SessionManager;
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
        server = new RpcServer(8765);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765);
        // 等待客户端连接成功
        client.connect().sync();
        // 设置为1000秒,到期后自动关闭session
        sessionManager = new SessionManager<>(10_000, (sessionId, session) -> {
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
            public RpcSessionSignature onSessionStart(RpcSessionContextWrapper contextWrapper, RpcSessionReactionWaiter waiter) {
                RpcSessionContext context = contextWrapper.getRpcSessionContext();
                RpcSession rpcSession = contextWrapper.getRpcSession();
                System.out.println("此次会话主题是:" + context.getTopic());
                if (true) {// 构建shell
                    BashSession bashSession = new BashSession();
                    bashSession.onOutPut(strs -> {
                        RpcReaction reaction = contextWrapper.getRpcSession().toReaction();
                        reaction.setBody(String.join("\n", strs));
                        waiter.sendReaction(reaction);
                    });
                    sessionManager.initSession(rpcSession.getSessionId(), bashSession);
                } else {
                    // 服务端判断不满足条件,直接关闭
                    return RpcSessionSignature.agree();
                }
                return RpcSessionSignature.agree();
            }

            @Override
            public void onReceiveRequest(RpcSessionContextWrapper contextWrapper, RpcSessionRequest request, RpcSessionReactionWaiter waiter) {
                // 假如客户端把命令写到body字段
                String command = request.getBody();
                BashSession session = sessionManager.getSession(contextWrapper.getRpcSession().getSessionId());
                if ("stop".equals(command)) {
                    session.close();
                } else {
                    // 将command也放入输出
                    session.sendCommand(command, true);

                }
            }

            @Override
            public void onSessionStop(RpcSessionContextWrapper contextWrapper, RpcSessionReactionWaiter waiter) {
                // 客户端要求停止session
                System.out.println("关闭session");
            }

            /**
             * 最终执行
             *
             * @param contextWrapper contextWrapper
             */
            public void onFinally(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {
                // 释放session资源--(release后,内部的在53行里面有个consumer,已经做了关闭,所以不顾要跟再做BashSession.close)
                sessionManager.release(contextWrapper.getRpcSession().getSessionId());
                System.out.println("关闭了释放了");
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
            System.out.println("服务端已开启session" + rpcSessionFuture.get().getReactionId());
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
//        client.sendSessionRequest(new RpcSessionRequest(session, "stop"));
//        Thread.sleep(1000);
//         切换了目录
        client.sendSessionRequest(new RpcSessionRequest(session, "cd /tmp"));
        Thread.sleep(1000);
        // 打印/tmp下的文件目录
        client.sendSessionRequest(new RpcSessionRequest(session, "ls -al"));
        // 防止线程退出
        Thread.currentThread().join();
    }
}
