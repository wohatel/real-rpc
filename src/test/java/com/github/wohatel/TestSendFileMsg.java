package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileSenderInput;
import com.github.wohatel.interaction.file.RpcFileSenderListener;
import com.github.wohatel.interaction.file.RpcFileSenderWrapper;
import com.github.wohatel.interaction.file.RpcFileTransModel;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * 发起文件的传输是相互的,服务端也可以向客户端发文件
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestSendFileMsg {

    private static RpcServer server;
    private static RpcDefaultClient client;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        RpcEventLoopManager eventLoopManager = RpcEventLoopManager.of(new NioEventLoopGroup());
        server = new RpcServer(8765, eventLoopManager);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765, eventLoopManager);
        // 等待客户端连接成功
        client.connect().sync();
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendFile() throws InterruptedException {

        server.onFileReceive(new RpcFileReceiverHandler() {

            /**
             * 收文件的一方,根据发送发发来的文件元数据信息,以及上下文信息决定如何处理文件
             */
            @Override
            public RpcFileLocal getTargetFile(RpcSession rpcSession, RpcSessionContext context, RpcFileInfo fileInfo) {
                String topic = context.getTopic();
                System.out.println("这个是啥文件:" + topic);
                long length = fileInfo.getLength();
                System.out.println("文件总大小为:" + length);
                File file = new File("/tmp/" + fileInfo.getFileName() + ".bak");
                // 我要求客户端断点续传的方式,如果该文件有了,就继续传
                RpcFileLocal local = new RpcFileLocal(file, RpcFileTransModel.RESUME);
                return local;
            }

            @Override
            public void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {
                System.out.println("服务端发话:传完了");
            }
        });
        RpcSessionContext rpcSessionContext = new RpcSessionContext();
        rpcSessionContext.setTopic("普通的keytab文件");

        // 此处可以限制速度,以及控制缓存大小,以及时间监听
        RpcFileSenderInput build = RpcFileSenderInput.builder().context(rpcSessionContext).rpcFileSenderListener(new RpcFileSenderListener() {
            @Override
            public void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {
                System.out.println("传输方式" + rpcFileSenderWrapper.getTransModel());
                System.out.println("客户端发话:成功");
            }

            @Override
            public void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {

            }
        }).build();
        client.sendFile(new File("/tmp/user.keytab"), build);

        // 防止线程退出
        Thread.currentThread().join();
    }

}
