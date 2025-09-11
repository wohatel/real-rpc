package com.github.wohatel;

import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.VirtualThreadPool;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.file.RpcFileTransModel;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendFileToClientTest {

    /**
     * 服务端主动发送文件到客户端
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
            RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
            rpcServer.onMsgReceive((cx, req) -> {
                if (req.getBody().equals("abcdef")) {
                    VirtualThreadPool.execute(() -> {
                        RpcSession rpcSession = new RpcSession(NumberConstant.TEN_EIGHT_K);
//                        new RpcFileTransConfig(NumberConstant.ONE_K, true);
                        RpcMsgTransUtil.writeFile(cx.channel(), new File("/Users/yaochuang/test/tilemaker.zip"), null);
                    });
                } else {
                    System.out.println(req);
                }

            });
            rpcServer.start();
        });
    }

    public static void clientConnect() {
        VirtualThreadPool.execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
            defaultClient.onFileReceive(new RpcFileReceiverHandler() {
                @Override
                public RpcFileLocal getTargetFile(final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo) {

                    System.out.println("收到了");

                    return new RpcFileLocal(new File("/Users/yaochuang/test/abcf94e83d9f75c2104596ffc3f20d5d247.zip"), RpcFileTransModel.APPEND);
//                    return null;
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
        });


    }

}
