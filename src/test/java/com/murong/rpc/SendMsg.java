package com.murong.rpc;

import com.murong.rpc.tcp.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.handler.RpcResponseMsgListener;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendMsg {


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
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
            defaultClient.connect();
            RpcFuture rpcFuture = defaultClient.sendSynMsg(new RpcRequest(), 10_000);
            rpcFuture.addListener(new RpcResponseMsgListener() {
                @Override
                public void onResponse(RpcResponse response) {
                    System.out.println(response);
                }

                /**
                 * 超时
                 */
                @Override
                public void onTimeout() {
                    System.out.println("超时");
                }
            });
            RpcResponse response = rpcFuture.get();
            System.out.println(response);

        });
    }

}
