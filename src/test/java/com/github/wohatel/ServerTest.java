package com.github.wohatel;

import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * description
 *
 * @author yaochuang 2025/03/21 14:07
 */
public class ServerTest {

    /**
     * 开启服务端
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
        rpcServer.start();
    }


}
