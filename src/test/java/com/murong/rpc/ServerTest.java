package com.murong.rpc;

import com.murong.rpc.server.RpcServer;

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
        RpcServer rpcServer = new RpcServer(8765);
        rpcServer.start();
    }


}
