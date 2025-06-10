package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileTransConfig;
import com.murong.rpc.interaction.file.RpcFileTransInterrupter;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.file.RpcFileWrapper;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcFileTransHandler;
import com.murong.rpc.server.RpcServer;
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
            rpcServer.setRpcFileRequestHandler(new RpcFileRequestHandler() {
                @Override
                public RpcFileWrapper getTargetFile(RpcFileContext context) {
                    System.out.println("收到了");
                    return new RpcFileWrapper(new File("/Users/yaochuang/test/abc123456123234.java"), RpcFileTransModel.REBUILD);
//                    return null;
                }

                /**
                 * 文件接收成功
                 *
                 * @param context 文件上下文
                 */
                @Override
                public void onSuccess(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper) {
                    System.out.println("完成了吗");
                }

                public void onProcess(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper, long recieveSize, RpcFileTransInterrupter interrupter) {
                    System.out.println("收到的总数:" + recieveSize);
                }

                @Override
                public void onStop(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper) {
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

            RpcFileTransHandler handler = new RpcFileTransHandler() {

                @Override
                public void onSuccess(File file, final RpcFileTransModel remoteTransModel) {
                    System.out.println(System.currentTimeMillis());
                }

            };
            RpcFileTransConfig config = new RpcFileTransConfig(100 * 1024 * 1024, 10 * 1024 * 1024, true);

            String sessionId = defaultClient.sendFile(new File("/Users/yaochuang/test/归档.zip"), null, handler, config);

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
