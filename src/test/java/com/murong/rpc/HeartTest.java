package com.murong.rpc;

import com.murong.rpc.client.RpcHeartClient;
import com.murong.rpc.server.RpcServer;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class HeartTest {


    /**
     * 心跳测试
     * 1: 开启server服务
     * 2: 开启client-->client内部开启了个线程,检测是否连接
     * 3: 计时5s后,关闭server
     * 5: 此时显示断连
     */
    public static void main(String[] args) throws InterruptedException {
        RpcServer rpcServer = serverStart();
        Thread.sleep(2000);
        clientConnect();
        Thread.sleep(20000);
        rpcServer.close();
    }

    public static RpcServer serverStart() {
        RpcServer rpcServer = new RpcServer(8765);
        rpcServer.start();
        return rpcServer;
    }

    public static void clientConnect() throws InterruptedException {
        RpcHeartClient heartclient = new RpcHeartClient("127.0.0.1", 8765);
        heartclient.connect();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("检测链接:" + heartclient.isAlived());
            }
        }).start();

    }
}
