package com.murong.rpc;

import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileLocal;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;

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
                public RpcFileLocal getTargetFile(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo) {
                    System.out.println("收到了");
                    System.out.println(fileInfo.getFileHash());
                    System.out.println("收到了");
                    return new RpcFileLocal(new File("/Users/yaochuang/test/abc" + rpcSession.getSessionId() + ".zip"));
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
            defaultClient.sendFile(new File("/Users/yaochuang/test/tilemaker.zip")).onSuccess(desc -> {
                System.out.println("结束");
            }).onProcess((desc, process) -> {
                System.out.println(process.getSendSize());
            });
        });


    }

}
