package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.common.RpcReactionWaiter;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileTransModel;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.tcp.RpcAutoReconnectClient;
import com.github.wohatel.tcp.RpcServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 发起文件的传输是相互的,服务端也可以向客户端发文件
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestServerSendFile {

    private static RpcServer server;
    private static RpcAutoReconnectClient client;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        RpcEventLoopManager eventLoopManager = RpcEventLoopManager.ofDefault();
        server = new RpcServer(8765, eventLoopManager);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcAutoReconnectClient("127.0.0.1", 8765, eventLoopManager);
        // 等待客户端连接成功
        client.autoReconnect();
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendFile() throws InterruptedException {

        Thread.sleep(2000);
        client.onFileReceive(new RpcFileRequestMsgHandler() {

            /**
             * 收文件的一方,根据发送发发来的文件元数据信息,以及上下文信息决定如何处理文件
             */
            @Override
            public RpcFileLocal getTargetFile(RpcSession rpcSession, RpcSessionContext context, RpcFileInfo fileInfo) {
                File file = new File("/tmp/" + fileInfo.getFileName() + ".bak");
                // 我要求客户端断点续传的方式,如果该文件有了,就继续传
                RpcFileLocal local = new RpcFileLocal(file, RpcFileTransModel.REBUILD);
                return local;
            }

            public void onProcess(final RpcFileReceiveWrapper rpcFileWrapper, long receivedSize, RpcFileInterrupter interrupter) {
                if (receivedSize > 1000) {
                    System.out.println("被中断");
                    interrupter.forceInterruptSession();
                }
            }

            public void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
                System.out.println("失败了");
            }

            @Override
            public void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {
                System.out.println("服务端发话:传完了");
            }
        });


        server.onRequestReceive(new RpcSimpleRequestMsgHandler() {
            @Override
            public void onReceiveRequest(RpcRequest request, RpcReactionWaiter waiter) {
                System.out.println("收到消息: 传输文件是个同步操作,占用同一个channel会造成线程卡死");
                Thread.ofVirtual().start(() -> {
                    waiter.sendFile(new File("/Users/yaochuang/tag-web/tag-webapp/pnpm-lock.yaml"), null);
                });
            }
        });


        // 此处可以限制速度,以及控制缓存大小,以及时间监听
        client.sendRequest(new RpcRequest());


        // 防止线程退出
        Thread.currentThread().join();
    }

}
