package com.github.wohatel;

import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.VirtualThreadPool;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileTransModel;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;
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
            RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
            rpcServer.onFileReceive(new RpcFileReceiverHandler() {
                @Override
                public RpcFileLocal getTargetFile(RpcSession rpcSession, RpcSessionContext context, RpcFileInfo rpcFileInfo) {
                    System.out.println("收到了");
                    return new RpcFileLocal(new File("/Users/yaochuang/test/abc123456123234.java"), RpcFileTransModel.REBUILD);
//                    return null;
                }

                /**
                 * 文件接收成功
                 *
                 */
                @Override
                public void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {
                    System.out.println("完成了吗");
                }

            });
            rpcServer.start();
        });
    }

    public static void clientConnect() {
        VirtualThreadPool.execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
            defaultClient.connect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            defaultClient.sendFile(new File("/Users/yaochuang/test/归档.zip"), null);

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
