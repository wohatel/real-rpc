package com.github.wohatel;

import com.github.wohatel.tcp.RpcAutoReconnectClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.tcp.strategy.ExponentialBackoffReconnectStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestAutoReconnect {

    private static RpcServer server;
    private static RpcAutoReconnectClient client;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        server = new RpcServer(8765);
        // 等待服务端开启成功
//        server.start().sync();
        client = new RpcAutoReconnectClient("127.0.0.1", 8765);
        client.setReconnectStrategy(new ExponentialBackoffReconnectStrategy(1000, 12_0000, () -> {
            System.out.println(System.currentTimeMillis());
            System.out.println("成功");
        }, () -> {
            System.out.println(System.currentTimeMillis());
            System.out.println("失败重连");
        }));
        // 等待客户端连接成功
        client.autoReconnect();
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    public void clientSendMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        Thread.sleep(5000l); //等待连接诶成功
        server.start().sync();
        // 客户度发送消息
//        client.sendRequest(RpcRequest.withBody("hello ketty"));
        // 防止线程退出
        Thread.currentThread().join();
    }

}
