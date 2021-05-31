package com.murong.rpc;


import com.murong.rpc.server.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Test;

public class ServerStart {

    @Test
    public void start() throws Exception {
        RpcServer rpcServer = new RpcServer(8888, new NioEventLoopGroup(), new NioEventLoopGroup());
        rpcServer.start();
    }

}
