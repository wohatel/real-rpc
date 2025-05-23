package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.server.RpcServer;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class WriteresponseTest {


    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(1000);
        clientConnect();
    }

    public static void serverStart() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.setRpcSimpleRequestMsgHandler((cx, req) -> {
                System.out.println("收到客户端:" + req.getBody());
                RpcRequest request = new RpcRequest();
                request.setBody("返回客户端的数据");
                RpcMsgTransUtil.sendMsg(cx.channel(), request);
                RpcFuture rpcFuture = RpcMsgTransUtil.sendSynMsg(cx.channel(), request);
                RpcResponse response = rpcFuture.get();
            });
            rpcServer.start();
        });
    }

    public static void clientConnect() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
            defaultClient.setRpcSimpleRequestMsgHandler((cx, req) -> {
                System.out.println(req.getBody());
            });

            defaultClient.connect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            RpcRequest request = new RpcRequest();
            request.setBody("abcdef");
            defaultClient.sendMsg(request);
        });


    }

}
