package com.github.wohatel;

import com.github.wohatel.interaction.common.RpcMutiEventLoopManager;
import com.github.wohatel.interaction.common.RpcSocketEventLoopManager;
import com.github.wohatel.tcp.RpcAutoReconnectClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.tcp.builder.RpcClientConnectConfig;
import com.github.wohatel.tcp.builder.RpcServerConnectConfig;
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

        client = new RpcAutoReconnectClient(RpcClientConnectConfig.builder().host("127.0.0.1").port(8765).build(), RpcSocketEventLoopManager.of(), new ExponentialBackoffReconnectStrategy(1000, 12_0000, () -> {
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

        Thread.sleep(5000l); //等待连接诶成功
        server = new RpcServer(RpcServerConnectConfig.builder().port(8765).build(), RpcMutiEventLoopManager.of());
        // 等待服务端开启成功
        server.start().sync();
        server.onRequestReceive((req, waiter) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        // 客户度发送消息
//        client.sendRequest(RpcRequest.withBody("hello ketty"));
        // 防止线程退出
        Thread.currentThread().join();
    }

}
