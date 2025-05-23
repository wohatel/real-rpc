package com.murong.rpc;

import com.murong.rpc.client.RpcHeartClient;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.server.RpcServer;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class HeartTest {


    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(2000);
        clientConnect();
    }

    public static void serverStart() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.start();
//            try {
////                Thread.sleep(10_000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            rpcServer.close();
        });
    }

    public static void clientConnect() throws InterruptedException {
        RpcHeartClient heartclient = new RpcHeartClient("127.0.0.1", 8765);
        heartclient.connect();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("检测异常:" + heartclient.isAlived());
            }
        }).start();

//        heartclient.close();

    }

}
