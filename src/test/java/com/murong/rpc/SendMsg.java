package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.handler.RpcResponseMsgListener;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.server.RpcServer;

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

    public static void serverStart() {

        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.start();
        });

    }

    public static void clientConnect() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
            defaultClient.connect();
            RpcFuture rpcFuture = defaultClient.sendSynMsg(new RpcRequest(),1000_1000);
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
