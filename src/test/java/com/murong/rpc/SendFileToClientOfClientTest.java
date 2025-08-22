package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileLocal;
import com.murong.rpc.interaction.file.RpcFileReceiveWrapper;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import io.netty.channel.ChannelHandlerContext;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendFileToClientOfClientTest {

    /**
     * 服务端主动发送文件到客户端
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        clientConnect();
    }


    public static void clientConnect() {

        RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
        defaultClient.onFileReceive(new RpcFileReceiverHandler() {
            @Override
            public RpcFileLocal getTargetFile(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context, RpcFileInfo fileInfo) {
                System.out.println("收到了");

                return new RpcFileLocal(new File("/Users/yaochuang/test/abc2a35cf7d-a84a-4f0b-b8c6-1dabe3406585.zip"), RpcFileTransModel.REBUILD);
            }

            @Override
            public void onProcess(ChannelHandlerContext ctx, RpcSession rpcSession, RpcFileReceiveWrapper rpcFileWrapper, long recieveSize) {
                System.out.println(recieveSize);
            }

            @Override
            public void onFailure(ChannelHandlerContext ctx, RpcSession rpcSession, RpcFileReceiveWrapper rpcFileWrapper, Exception e) {
                RpcFileReceiverHandler.super.onFailure(ctx, rpcSession,  rpcFileWrapper, e);
            }

            @Override
            public void onSuccess(ChannelHandlerContext ctx, RpcSession rpcSession, RpcFileReceiveWrapper rpcFileWrapper) {
                System.out.println();
            }

            /**
             * 远端发出终止传输信号
             *
             * @param context 文件上下文
             */
            public void onStop(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context, final RpcFileReceiveWrapper rpcFileWrapper) {
                System.out.println("发送端终止:");
            }
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


    }

}
