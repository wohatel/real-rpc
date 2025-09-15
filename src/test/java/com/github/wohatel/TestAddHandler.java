package com.github.wohatel;

import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 *
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestAddHandler {

    private static RpcServer server;
    private static RpcDefaultClient client;
    private static MultiThreadIoEventLoopGroup group;

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {

        ChannelInboundHandlerAdapter adapter = new ChannelInboundHandlerAdapter() {
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                System.out.println("channel active");
                ctx.fireChannelActive();
            }
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                System.out.println("channel channelInactive");
                ctx.fireChannelInactive();
            }
        };


        // 线程组暂时用一个
        group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        server = new RpcServer(8765, group, group);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765, group);
        // 等待客户端连接成功




        ChannelFuture future = client.connect().sync();


        ChannelPipeline pipeline = future.channel().pipeline();

        List<String> names = pipeline.names();
        System.out.println(names);


        pipeline.addAfter("encoder", "simple", adapter);

        // 绑定服务端接收消息处理
        server.onMsgReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        // 客户度发送消息
        client.sendMsg(RpcRequest.withBody("hello ketty"));
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
        server.onMsgReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("服务端收到消息体:" + body);
            // 返回消息体
            if (req.isNeedResponse()) {
                RpcResponse response = req.toResponse();
                response.setBody("thanks, got it");
                RpcMsgTransUtil.write(ctx.channel(), response);
            }
        });
        // 客户度发送消息
        RpcFuture sendFuture = client.sendSynMsg(RpcRequest.withBody("hello ketty"));
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
        server.onMsgReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("服务端收到-客户端招呼:" + body);


            System.out.println("服务端开始向客户端发送请求:--------");
            // 服务端向客户端发消息
            RpcRequest rpcRequest = RpcRequest.withBody("近来你还好吧?");
            RpcMsgTransUtil.sendMsg(ctx.channel(), rpcRequest);


            // 此处只是做了简单问候,也可发送后等待客户端回应()
            // rpcRequest.setNeedResponse(true); 客户端也需要判断该字段,进行返回结果
            // RpcMsgTransUtil.sendSynMsg(ctx.channel(), rpcRequest);
        });

        // 客户端收到消息后如何处理
        client.onMsgReceive((ctx, req) -> {
            // 客户端收到消息
            String body = req.getBody();
            System.out.println("客户端收到服务端消息:" + body);
        });

        // 客户端先问候
        client.sendMsg(RpcRequest.withBody("hello ketty"));


        // 防止线程退出
        Thread.currentThread().join();
    }
}
