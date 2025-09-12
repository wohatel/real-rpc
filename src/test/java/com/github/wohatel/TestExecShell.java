package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.BashSession;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.SessionManager;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Random;


/**
 * description
 *
 * @author yaochuang 2025/09/09 15:06
 */
public class TestExecShell {

    private static RpcServer server;
    private static RpcDefaultClient client;
    private static MultiThreadIoEventLoopGroup group;
    private static SessionManager<BashSession> sessionManager;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        server = new RpcServer(8765, group, group);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765, group);
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
            public void sessionStart(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                System.out.println("此次会话主题是:" + context.getTopic());
                if (true) {// 构建shell
                    BashSession bashSession = new BashSession(str -> {
                        // 此处以response的方式返回,接收方需要以future.addListener 方式监听
                        // 也可以用request的方式返回,但是另外一端需要以处理请求的方式
                        RpcResponse response = rpcSession.toResponse();
                        response.setBody(str);
                        RpcMsgTransUtil.write(ctx.channel(), response);
                    });
                    sessionManager.initSession(rpcSession.getSessionId(), bashSession);
                }

            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request, RpcSessionContext context) {
                // 假如客户端把命令写到body字段
                String command = request.getBody();
                BashSession session = sessionManager.getSession(rpcSession.getSessionId());
                session.sendCommand(command);
            }

            @Override
            public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                System.out.println("关闭session");
            }
        };

        server.onSessionMsgReceive(serverSessionHandler);

        // 这次会话最多-一旦30s内没有人说话,就算是被中断了
        RpcSession session = new RpcSession(30_000);
        // 请求开启会谈
        RpcSessionContext rpcSessionContext = new RpcSessionContext();
        rpcSessionContext.setTopic("开启shell");
        RpcSessionFuture rpcSessionFuture = client.startSession(session, rpcSessionContext);
        // 此处接收response的数据
        rpcSessionFuture.addListener(response -> {
            if (response.isSuccess()) {
                String body = response.getBody();
                System.out.println("打印response:" + body);
            }
        });

        // 打印工作目录下的文件列表
        client.sendSessionMsg(new RpcSessionRequest(session, "ls -al"));
        // 切换了目录
        client.sendSessionMsg(new RpcSessionRequest(session, "cd /tmp"));
        // 打印/tmp下的文件目录
        client.sendSessionMsg(new RpcSessionRequest(session, "ls -al"));

        // 防止线程退出
        Thread.currentThread().join();
    }
}
