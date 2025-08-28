package com.murong.rpc;

import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileSenderInput;
import com.murong.rpc.interaction.file.RpcFileSenderListener;
import com.murong.rpc.interaction.file.RpcFileSenderWrapper;
import com.murong.rpc.interaction.file.RpcFileTransProcess;
import com.murong.rpc.server.RpcServer;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendFileToClientOfServerTest {

    /**
     * 服务端主动发送文件到客户端
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        serverStart();
    }

    public static void serverStart() {
        RpcServer rpcServer = new RpcServer(8765);
        rpcServer.onMsgReceive((cx, req) -> {
            if (req.getBody().equals("abcdef")) {
                VirtualThreadPool.execute(() -> {
                    RpcFileSenderInput.RpcFileSenderInputBuilder builder = RpcFileSenderInput.builder();
                    builder.rpcFileSenderListener(new RpcFileSenderListener() {
                        @Override
                        public void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {
                            System.out.println("结束");
                        }

                        @Override
                        public void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {

                        }

                        @Override
                        public void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {

                        }
                    });
                    RpcMsgTransUtil.writeFile(cx.channel(), new File("/Users/yaochuang/test/tilemaker.zip"), builder.build());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 主动终止
//                    RpcMsgTransUtil.writeStopFile(cx.channel(), rpcSession);

                });
            } else {
                System.out.println(req + "rtyuio");
            }

        });
        rpcServer.start();

    }


}
