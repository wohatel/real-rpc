package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionReactionWaiter;
import com.github.wohatel.interaction.file.RpcSessionSignature;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * ----------注意--------------
 * 只能客户端发起会话及请求,服务端响应
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestSendSessionMsg {

    private static RpcServer server;
    private static RpcDefaultClient client;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        server = new RpcServer(8765);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765);
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
            public RpcSessionSignature signature(RpcSessionContextWrapper contextWrapper, RpcSessionReactionWaiter waiter) {
                System.out.println("服务端收到客户端会话请求:");
                System.out.println("此次会话主题是:" + contextWrapper.getRpcSessionContext().getTopic());
                // 同意开启会话
                return RpcSessionSignature.agree();
            }

            @Override
            public void onReceiveRequest(RpcSessionContextWrapper contextWrapper, RpcSessionRequest request, RpcSessionReactionWaiter waiter) {
                // 服务端收到客户端消息后,直接打印
                System.out.println(request.getBody());
                // 打印归打印,什么时候发消息看我心情看我心情

                RpcReaction reaction = request.getRpcSession().toReaction();
                reaction.setBody("不想理你!!!");
                waiter.sendReaction(reaction);
            }

            @Override
            public void onSessionStop(RpcSessionContextWrapper contextWrapper, RpcSessionReactionWaiter waiter) {
                System.out.println("服务端-------电话被挂断-----------");
            }

            @Override
            public void onFinally(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {
                System.out.println("此次会话结束后,需要清理会场");
            }
        };

        server.onSessionRequestReceive(serverSessionHandler);
        // 客户端设置会话
        // 这次会话最多-一旦30s内没有人说话,就算是被中断了
        RpcSession session = new RpcSession(30_000);
        // 请求开启会谈
        RpcSessionContext rpcSessionContext = new RpcSessionContext();
        rpcSessionContext.setTopic("还钱的事");
        RpcSessionFuture rpcSessionFuture = client.startSession(session, rpcSessionContext);
        RpcReaction rpcReaction = rpcSessionFuture.get();
        boolean success = rpcReaction.isSuccess();
        if (success) {
            System.out.println("服务端已同意开启会话");
        } else {
            System.out.println("服务端不同意开启:" + rpcReaction.getMsg());
        }

        /**
         * 监听服务端小消息
         */
        rpcSessionFuture.addListener((future) -> {
            System.out.println("收到响应消息");
            if (future.isSuccess()) {
                System.out.println(future.getBody());
            }else {
                System.out.println(future.getMsg());
            }
        });

        client.sendSessionRequest(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionRequest(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionRequest(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionRequest(new RpcSessionRequest(session, "你什么时间还钱"));
        client.sendSessionRequest(new RpcSessionRequest(session, "你什么时间还钱"));

        // 客户端觉得再追问也没结果,沉默1s后,挂断电话
        Thread.sleep(10000);
        client.stopSession(session);

        boolean sessionFinish = rpcSessionFuture.getRpcSessionProcess() == RpcSessionProcess.FINISHED;
        System.out.println("打印当前会话是否结束:" + sessionFinish);

        // 防止线程退出
        Thread.currentThread().join();
    }

}
