package com.murong.rpc;

import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileWrapper;
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
        VirtualThreadPool.execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.setRpcFileRequestHandler(new RpcFileRequestHandler() {
                @Override
                public RpcFileWrapper getTargetFile(RpcFileContext context) {
                    System.out.println("收到了");
                    String id = context.getRpcSession().getSessionId();
                    return new RpcFileWrapper(new File("/Users/yaochuang/test/abc" + id + ".zip"));
                }
            });
            rpcServer.start();
        });
    }

    public static void clientConnect() {
        VirtualThreadPool.execute(() -> {
            RpcAutoReconnectClient defaultClient = new RpcAutoReconnectClient("127.0.0.1", 8765);
            defaultClient.autoReconnect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            defaultClient.sendFile(new File("/Users/yaochuang/test/tilemaker.zip"));
        });


    }

}
