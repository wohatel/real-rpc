package com.murong.rpc;


import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.server.RpcServer;

import java.util.concurrent.atomic.AtomicLong;

public class ReconnectClientTest {

    /**
     * 断线重连测试
     * 1: 开启server服务
     * 2: 开启client
     * 3: 计时3s后,client的链接
     * 5: 此时显示断连,但很快重连上
     */
    public static void main(String[] args) throws InterruptedException {
        serverStart();
        RpcAutoReconnectClient test = test();
        Thread.sleep(3000);
        test.getChannel().close();
    }


    public static RpcServer serverStart() {
        RpcServer rpcServer = new RpcServer(8765);
        rpcServer.start();
        return rpcServer;
    }

    public static RpcAutoReconnectClient test() {
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8765);
        client.autoReconnect();
        return client;
    }


}
