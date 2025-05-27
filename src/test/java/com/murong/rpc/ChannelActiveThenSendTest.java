package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.handler.RpcResponseMsgListener;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class ChannelActiveThenSendTest {


    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(4000);
        clientConnect();
    }

    /**
     * 发送消息测试
     */
    public static void serverStart() {

        VirtualThreadPool.execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.start();
        });

    }

    public static void clientConnect() {
        VirtualThreadPool.execute(() -> {
            ChannelInboundHandlerAdapter channelInboundHandlerAdapter = new ChannelInboundHandlerAdapter() {
                @Override
                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    Thread.startVirtualThread(() -> {// 注意异步的发送,千万不要阻塞线程池处理的逻辑

                        System.out.println("连接上就开始发送");
                        RpcFuture rpcFuture = RpcMsgTransUtil.sendSynMsg(ctx.channel(), new RpcRequest());
                        RpcResponse rpcResponse = rpcFuture.get();
                        System.out.println(rpcResponse);
                        ctx.fireChannelActive();
                    });

                }
            };


            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new RpcMsgChannelInitializer(p -> p.addLast(channelInboundHandlerAdapter)));
            defaultClient.connect();

        });
    }

}
