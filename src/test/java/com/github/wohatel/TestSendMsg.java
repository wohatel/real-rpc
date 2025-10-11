package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
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
    private static MultithreadEventLoopGroup group;

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
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((ctx, req) -> {
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
        server.onRequestReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("服务端收到消息体:" + body);
            // 返回消息体
            if (req.isNeedResponse()) {
                RpcResponse response = req.toResponse();
                response.setBody("thanks, got it");
                RpcMsgTransManager.sendResponse(ctx.channel(), response);
            }
        });
        // 客户度发送消息
        RpcFuture sendFuture = client.sendSynRequest(RpcRequest.withBody("hello ketty"));
        RpcResponse rpcResponse = sendFuture.get();
        String body = rpcResponse.getBody();

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
        server.onRequestReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("服务端收到-客户端招呼:" + body);


            System.out.println("服务端开始向客户端发送请求:--------");
            // 服务端向客户端发消息
            RpcRequest rpcRequest = RpcRequest.withBody("近来你还好吧?");
            RpcMsgTransManager.sendRequest(ctx.channel(), rpcRequest);


            // 此处只是做了简单问候,也可发送后等待客户端回应()
            // rpcRequest.setNeedResponse(true); 客户端也需要判断该字段,进行返回结果
            // RpcMsgTransManager.sendSynMsg(ctx.channel(), rpcRequest);
        });

        // 客户端收到消息后如何处理
        client.onRequestReceive((ctx, req) -> {
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
