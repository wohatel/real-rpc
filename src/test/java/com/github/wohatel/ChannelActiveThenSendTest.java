package com.github.wohatel;

import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.VirtualThreadPool;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;

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
            RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
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


            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
            defaultClient.connect();

        });
    }

}
