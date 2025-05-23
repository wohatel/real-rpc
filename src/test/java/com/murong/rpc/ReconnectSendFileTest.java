package com.murong.rpc;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileWrapper;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.server.RpcServer;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class ReconnectSendFileTest {


    /**
     * 文件传输测试-- 重建
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(1000);
        clientConnect();
    }

    public static void serverStart() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.setRpcFileRequestHandler(new RpcFileRequestHandler() {
                @Override
                public RpcFileWrapper getTargetFile(RpcFileContext context) {
                    System.out.println(context.getContext());
                    System.out.println("收到了");
                    String id = context.getSessionId();
                    return new RpcFileWrapper(new File("/Users/yaochuang/test/abc" + id + ".zip"));
//                    return null;
                }
            });
            rpcServer.start();
        });
    }

    public static void clientConnect() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcAutoReconnectClient defaultClient = new RpcAutoReconnectClient("127.0.0.1", 8765);
            defaultClient.autoReconnect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("a", "a");
            defaultClient.sendFile(new File("/Users/yaochuang/test/tilemaker.zip"), jsonObject);
        });


    }

}
