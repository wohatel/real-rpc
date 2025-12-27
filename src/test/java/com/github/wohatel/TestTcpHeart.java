package com.github.wohatel;

import com.github.wohatel.interaction.common.RpcVivoHandler;
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

    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {

        RpcVivoHandler rpcVivoHandler = new RpcVivoHandler(10_000, 3000) {

            @Override
            public void onChannelHeatTimeOut(ChannelHandlerContext ctx) {
                System.out.println("超时");
            }

            @Override
            public void onChannelActive(ChannelHandlerContext ctx) {
                System.out.println("连接成功");
            }

            @Override
            public void onChannelInactive(ChannelHandlerContext ctx) {
                System.out.println("连接断开");
            }
        };

        // 线程组暂时用一个
        server = new RpcServer(8765, rpcVivoHandler);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765);
        // 等待客户端连接成功
        client.connect().sync();
        // 绑定服务端接收消息处理
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        for (int j = 0; j < 15; j++) {
            Thread.sleep(1000);
            if (j == 14) {
                client.close();
            }
        }
        Thread.currentThread().join();
    }

}
