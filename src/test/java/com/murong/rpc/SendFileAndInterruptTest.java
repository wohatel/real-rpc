package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileLocal;
import com.murong.rpc.interaction.file.RpcFileTransWrapper;
import com.murong.rpc.interaction.file.RpcFileRemote;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import com.murong.rpc.interaction.handler.RpcFileSenderHandler;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.PlatformDependent;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendFileAndInterruptTest {


    /**
     * 文件测试: --追加模式
     * 同时打印传输进度
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
            rpcServer.setRpcFileRequestHandler(new RpcFileReceiverHandler() {
                @Override
                public RpcFileLocal getTargetFile(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context, RpcFileInfo rpcFileInfo) {
                    System.out.println("收到了");
                    return new RpcFileLocal(new File("/Users/yaochuang/test/abc123456123234.java"), RpcFileTransModel.REBUILD);
//                    return null;
                }

                /**
                 * 文件接收成功
                 *
                 */
                @Override
                public void onSuccess(ChannelHandlerContext ctx, RpcSession rpcSession, final RpcFileTransWrapper rpcFileWrapper) {
                    System.out.println("完成了吗");
                }

                @Override
                public void onStop(ChannelHandlerContext ctx, RpcSession rpcSession, final RpcFileTransWrapper rpcFileWrapper) {
                    System.out.println("传输方结束结束了");
                }

            });
            rpcServer.start();
        });
    }

    public static void clientConnect() {
        VirtualThreadPool.execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
            defaultClient.connect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            RpcFileSenderHandler handler = new RpcFileSenderHandler() {

                @Override
                public void onSuccess(File file, final RpcFileRemote rpcFileRemoteWrapper) {
                    System.out.println(System.currentTimeMillis());
                    System.out.println(rpcFileRemoteWrapper.getFilePath());
                }

            };
            defaultClient.sendFile(new File("/Users/yaochuang/test/归档.zip"), new RpcSession(10_000), null, handler);

            try {
                Thread.sleep(10000l);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
//            defaultClient.interruptSendFile(sessionId);
        });


    }


    public static void test() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            long used = PlatformDependent.usedDirectMemory();
            System.out.printf("Used Direct Memory: %.2f MB%n", used / (1024.0 * 1024));
        }, 0, 1, TimeUnit.SECONDS);
    }

}
