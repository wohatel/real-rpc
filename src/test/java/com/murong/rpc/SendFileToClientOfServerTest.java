package com.murong.rpc;

import com.murong.rpc.interaction.file.RpcFileSenderWrapper;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.VirtualThreadPool;
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
                    RpcFileSenderWrapper rpcFileSenderWrapper = RpcMsgTransUtil.writeFile(cx.channel(), new File("/Users/yaochuang/test/tilemaker.zip"), null);
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
