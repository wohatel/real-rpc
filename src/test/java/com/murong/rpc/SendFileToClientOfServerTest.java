package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcSessionContext;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileTransInterrupter;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.file.RpcFileWrapper;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
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
        rpcServer.setRpcSimpleRequestMsgHandler((cx, req) -> {
            if (req.getBody().equals("abcdef")) {
                VirtualThreadPool.execute(() -> {
                    RpcSessionContext sessionContext = new RpcSessionContext("1", "1", "2", "3");
                    RpcMsgTransUtil.writeFile(cx.channel(), new File("/Users/yaochuang/test/tilemaker.zip"), sessionContext, (f, transModel, t) -> {
                        System.out.println(f);
                    });
                });
            } else {
                System.out.println(req);
            }

        });
        rpcServer.start();

    }


}
