package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.RpcMutiEventLoopManager;
import com.github.wohatel.interaction.common.RpcSocketEventLoopManager;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.tcp.builder.RpcClientConnectConfig;
import com.github.wohatel.tcp.builder.RpcServerConnectConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestSendMsg {

    private static RpcServer server;
    private static RpcDefaultClient client;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        server = new RpcServer(RpcServerConnectConfig.builder().port(8765).build(), RpcMutiEventLoopManager.of());
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient(RpcClientConnectConfig.builder().host("127.0.0.1").port(8765).build(), RpcSocketEventLoopManager.of());
        // 等待客户端连接成功
        client.connect().sync();
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        // 客户度发送消息
        client.sendRequest(RpcRequest.withBody("hello ketty"));
        // 防止线程退出
        Thread.currentThread().join();
    }

    /**
     * 客户端发送消息--并同步接收服务端的消息
     *
     */
    @Test
    void clientSendAndReceiveMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("服务端收到消息体:" + body);
            // 返回消息体
            if (req.isNeedReaction()) {
                RpcReaction reaction = req.toReaction();
                reaction.setBody("thanks, got it");
                waiter.sendReaction(reaction);
            }
        });
        // 客户度发送消息
        RpcFuture sendFuture = client.sendSynRequest(RpcRequest.withBody("hello ketty"));
        RpcReaction rpcReaction = sendFuture.get();
        String body = rpcReaction.getBody();

        System.out.println("客户端收到服务端的应答:" + body);
        // 防止线程退出
        Thread.currentThread().join();
    }


    /**
     * 服务端也可主动向客户端发送消息
     */
    @Test
    void serverSendMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("服务端收到-客户端招呼:" + body);


            System.out.println("服务端开始向客户端发送请求:--------");
            // 服务端向客户端发消息
            RpcRequest rpcRequest = RpcRequest.withBody("近来你还好吧?");
            waiter.sendRequest(rpcRequest);

        });

        // 客户端收到消息后如何处理
        client.onRequestReceive((req, waiter) -> {
            // 客户端收到消息
            String body = req.getBody();
            System.out.println("客户端收到服务端消息:" + body);
        });

        // 客户端先问候
        client.sendRequest(RpcRequest.withBody("hello ketty"));

        // 防止线程退出
        Thread.currentThread().join();
    }
}
