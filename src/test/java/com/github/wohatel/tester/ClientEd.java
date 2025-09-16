package com.github.wohatel.tester;

import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.tcp.RpcAutoReconnectClient;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;

/**
 * description
 *
 * @author yaochuang 2025/09/15 14:26
 */
public class ClientEd {


    public static void main(String[] args) throws InterruptedException {
        MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8765, group);

        client.autoReconnect();
        Thread.sleep(1000);
        RpcSession session = new RpcSession(10000);
        RpcSessionFuture rpcSessionFuture = client.startSession(session);
        RpcResponse rpcResponse = rpcSessionFuture.get();
        client.sendSessionMsg(new RpcSessionRequest(session));

        Thread.currentThread().join();
    }
}
