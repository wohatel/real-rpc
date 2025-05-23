package com.murong.rpc;


import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;

import java.util.concurrent.atomic.AtomicLong;

public class ReconnectClientTest {

    public static void test() throws InterruptedException {
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8765);
        client.setOnReconnectSuccess(c->{
            System.out.println("链接成功了");
        });
        client.autoReconnect();

        Thread.sleep(1000);
        client.close();

//        AtomicLong atomicLong = new AtomicLong();
//        ThreadUtil.run(30, () -> {
//            long l1 = System.currentTimeMillis();
//            while (true) {
//                try {
//                    StringBuilder sb = new StringBuilder("我");
//                    RpcRequest rpcFileRequest = new RpcRequest();
//                    rpcFileRequest.setBody(sb.toString());
//                    long l = atomicLong.addAndGet(1l);
//                    if (l % 1000 == 0) {
////                        RpcFuture rpcFuture = client.sendSynMsg(rpcFileRequest);
////                        RpcResponse rpcResponse = rpcFuture.get();
////                        System.out.println(rpcResponse.getBody());
//                        long l2 = System.currentTimeMillis();
//                        System.out.println(l * 1000 / (l2 - l1));
//                        client.close();
//                    } else {
//                        RpcFuture rpcFuture = client.sendSynMsg(rpcFileRequest);
//                        RpcResponse rpcResponse = rpcFuture.get();
//                        System.out.println(rpcResponse);
//                    }
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }

    public static void main(String[] args) throws InterruptedException {
        test();
    }

}
