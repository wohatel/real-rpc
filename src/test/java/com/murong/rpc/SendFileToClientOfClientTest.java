package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileLocalWrapper;
import com.murong.rpc.interaction.file.RpcFileLocalWrapperImpl;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
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
        defaultClient.setRpcFileRequestHandler(new RpcFileRequestHandler() {
            @Override
            public RpcFileLocalWrapper getTargetFile(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context, RpcFileInfo fileInfo) {
                System.out.println("收到了");

                return new RpcFileLocalWrapper(new File("/Users/yaochuang/test/abcf94e83d9f75c2104596ffc3f20d5d247.zip"), RpcFileTransModel.REBUILD);
            }

            @Override
            public void onProcess(ChannelHandlerContext ctx, RpcSession rpcSession,RpcFileLocalWrapperImpl rpcFileWrapper, long recieveSize) {
                System.out.println(recieveSize);
            }

            @Override
            public void onFailure(ChannelHandlerContext ctx, RpcSession rpcSession, RpcFileLocalWrapperImpl rpcFileWrapper, Exception e) {
                RpcFileRequestHandler.super.onFailure(ctx, rpcSession,  rpcFileWrapper, e);
            }

            @Override
            public void onSuccess(ChannelHandlerContext ctx, RpcSession rpcSession, RpcFileLocalWrapperImpl rpcFileWrapper) {
                RpcFileRequestHandler.super.onSuccess(ctx, rpcSession, rpcFileWrapper);
            }

            /**
             * 远端发出终止传输信号
             *
             * @param context 文件上下文
             */
            public void onStop(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context, final RpcFileLocalWrapperImpl rpcFileWrapper) {
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
