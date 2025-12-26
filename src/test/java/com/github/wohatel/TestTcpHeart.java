package com.github.wohatel;

import com.github.wohatel.interaction.common.RpcHeartHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author yaochuang 2025/09/28 11:15
 */
public class TestTcpHeart {


    private static RpcServer server;
    private static RpcDefaultClient client;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        RpcHeartHandler rpcHeartHandler = new RpcHeartHandler(10_000, 3000) {

            @Override
            public void onChannelHeatTimeOut(ChannelHandlerContext ctx) throws Exception {
                System.out.println("超时");
            }
        };

        // 线程组暂时用一个
        server = new RpcServer(8765);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765, rpcHeartHandler);
        // 等待客户端连接成功
        client.connect().sync();
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });

        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
//                client.sendRequest(RpcRequest.withBody("你好"));
//                RpcMsgTransManager.sendHeart(client.getChannel(), RpcHeartAction.PING);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });


        Thread.currentThread().join();
    }

}
