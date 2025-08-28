package com.murong.rpc;

import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileLocal;
import com.murong.rpc.interaction.file.RpcFileReceiveWrapper;
import com.murong.rpc.interaction.file.RpcFileSenderInput;
import com.murong.rpc.interaction.file.RpcFileSenderListener;
import com.murong.rpc.interaction.file.RpcFileSenderWrapper;
import com.murong.rpc.interaction.file.RpcFileTransProcess;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
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
            rpcServer.onFileReceive(new RpcFileReceiverHandler() {
                @Override
                public RpcFileLocal getTargetFile(final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo) {
                    return new RpcFileLocal(new File("/Users/yaochuang/test/abc" + rpcSession.getSessionId() + ".zip"));
                }

                @Override
                public void onProcess(RpcFileReceiveWrapper rpcFileWrapper, long recieveSize) {
                    System.out.println(recieveSize);
                    if (recieveSize > 2 * 1024 * 1024) {
                        System.out.println("尝试中断");
                        rpcFileWrapper.interruptReceive();
                    }
                }

                /**
                 * 文件接收异常执行
                 *
                 * @param e 发生的异常
                 */
                public void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
                    System.out.println("接收端失败");
                }

                /**
                 * 文件整体传输完毕
                 *
                 */
                public void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {
                    System.out.println("接收端成功");
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
            RpcFileSenderInput.RpcFileSenderInputBuilder builder = RpcFileSenderInput.builder();
            builder.rpcFileSenderListener(new RpcFileSenderListener() {
                @Override
                public void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {
                    System.out.println("结束");
                }

                @Override
                public void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {
                    System.out.println("出现异常信息,请注意");
                }

                @Override
                public void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {

                }
            });
            defaultClient.sendFile(new File("/Users/yaochuang/test/tilemaker.zip"), builder.build());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });


    }

}
