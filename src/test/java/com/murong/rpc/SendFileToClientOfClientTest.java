package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileTransInterrupter;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.file.RpcFileWrapper;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;

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
            public RpcFileWrapper getTargetFile(RpcFileContext context) {

                System.out.println("收到了");
                String id = context.getRpcSession().getSessionId();
                System.out.println(id);
                return new RpcFileWrapper(new File("/Users/yaochuang/test/abcf94e83d9f75c2104596ffc3f20d5d247.zip"), RpcFileTransModel.APPEND);
//                    return null;
            }

            @Override
            public void onProcess(final RpcFileContext context, RpcFileWrapper rpcFileWrapper, long recieveSize, RpcFileTransInterrupter interrupter) {
                System.out.println("接收大小:" + recieveSize + " 总大小:" + context.getLength());
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
