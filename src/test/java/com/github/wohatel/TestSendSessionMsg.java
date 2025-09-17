package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * ----------注意--------------
 * 只能客户端发起会话及请求,服务端响应
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestSendSessionMsg {

    private static RpcServer server;
    private static RpcDefaultClient client;
    private static MultiThreadIoEventLoopGroup group;

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
            public boolean sessionStart(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                System.out.println("服务端收到客户端会话请求:");
                System.out.println("此次会话主题是:" + context.getTopic());
                // 同一开启会话
                return true;
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request, RpcSessionContext context) {
                // 服务端收到客户端消息后,直接打印
                System.out.println(request.getBody());
                // 打印归打印,什么时候发消息看我心情看我心情
                if (new Random().nextInt() % 3 == 0) {
                    RpcResponse response = rpcSession.toResponse();
                    response.setBody("不想理你!!!");
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }

            }

            @Override
            public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                System.out.println("服务端-------电话被挂断-----------");
            }
        };

        server.onSessionMsgReceive(serverSessionHandler);
        // 客户端设置会话
        // 这次会话最多-一旦30s内没有人说话,就算是被中断了
        RpcSession session = new RpcSession(30_000);
        // 请求开启会谈
        RpcSessionContext rpcSessionContext = new RpcSessionContext();
        rpcSessionContext.setTopic("还钱的事");
        RpcSessionFuture rpcSessionFuture = client.startSession(session, rpcSessionContext);
        /**
         * 监听服务端小消息
         */
        rpcSessionFuture.addListener((future) -> {
            if (future.isSuccess()) {
                System.out.println(future.getBody());
            }
        });

        client.sendSessionMsg(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionMsg(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionMsg(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionMsg(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionMsg(new RpcSessionRequest(session, "你什么时间还钱"));

        // 客户端觉得再追问也没结果,沉默1s后,挂断电话
        Thread.sleep(1000);
        client.finishSession(session);

        boolean sessionFinish = rpcSessionFuture.isSessionFinish();
        System.out.println("打印当前会话是否结束:" + sessionFinish);

        // 防止线程退出
        Thread.currentThread().join();
    }

}
