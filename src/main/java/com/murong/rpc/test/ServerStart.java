package com.murong.rpc.test;


import com.murong.rpc.server.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;

public class ServerStart {

    public static void main(String[] args) throws Exception {
        RpcServer rpcServer = new RpcServer(8888, new NioEventLoopGroup(), new NioEventLoopGroup());
        rpcServer.start();
    }
}
