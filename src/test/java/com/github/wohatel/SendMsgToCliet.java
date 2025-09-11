package com.github.wohatel;

import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.common.VirtualThreadPool;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendMsgToCliet {


    /**
     * 服务端向客户端发送消息
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(2000);
        clientConnect();
    }

    public static void serverStart() {

VirtualThreadPool.execute(() -> {
            RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
            rpcServer.onMsgReceive((ctx, req) -> {
                System.out.println("开始");
                RpcRequest request = new RpcRequest();
                RpcFuture rpcFuture = RpcMsgTransUtil.sendSynMsg(ctx.channel(), request);
                rpcFuture.setTimeOut(300_000);
                System.out.println(rpcFuture);
                RpcResponse response = rpcFuture.get();

                System.out.println(response.getBody() + ":结果结果");

            });
            rpcServer.start();
        });

    }

    public static void clientConnect() {
VirtualThreadPool.execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
            defaultClient.onMsgReceive((ctx, req) -> {
                RpcResponse response = req.toResponse();
                response.setBody("这是响应结果");
                RpcMsgTransUtil.write(ctx.channel(), response);
            });
            defaultClient.connect();
            defaultClient.sendMsg(new RpcRequest());

        });
    }

}
