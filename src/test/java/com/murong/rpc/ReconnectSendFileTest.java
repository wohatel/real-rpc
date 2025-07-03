package com.murong.rpc;

import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileLocalWrapper;
import com.murong.rpc.interaction.file.RpcFileRemoteWrapper;
import com.murong.rpc.interaction.file.RpcFileTransConfig;
import com.murong.rpc.interaction.file.RpcFileTransProcess;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcFileTransHandler;
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
                public RpcFileLocalWrapper getTargetFile(RpcFileContext context) {
                    System.out.println("收到了");
                    String id = context.getRpcSession().getSessionId();
                    return new RpcFileLocalWrapper(new File("/Users/yaochuang/test/abc" + id + ".zip"));
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
            RpcFileTransHandler handler = new RpcFileTransHandler() {
                @Override
                public void onProcess(File file, RpcFileRemoteWrapper rpcFileRemoteWrapper, RpcFileTransProcess rpcFileTransProcess) {
                    RpcFileTransHandler.super.onProcess(file, rpcFileRemoteWrapper, rpcFileTransProcess);
                }

                @Override
                public void onFailure(File file, RpcFileRemoteWrapper rpcFileRemoteWrapper, String errorMsg) {
                    RpcFileTransHandler.super.onFailure(file, rpcFileRemoteWrapper, errorMsg);
                }

                @Override
                public void onSuccess(File file, RpcFileRemoteWrapper rpcFileRemoteWrapper) {
                    System.out.println(rpcFileRemoteWrapper.getFilePath());
                    System.out.println(rpcFileRemoteWrapper.getFilePath());
                    System.out.println(rpcFileRemoteWrapper.getFilePath());
                    System.out.println(rpcFileRemoteWrapper.getFilePath());
                }
            };
            RpcFileTransConfig config = new RpcFileTransConfig(100 * 1024 * 1024l, true);

            defaultClient.sendFile(new File("/Users/yaochuang/test/tilemaker.zip"), new RpcSession(10000), null, handler, config);
        });


    }

}
