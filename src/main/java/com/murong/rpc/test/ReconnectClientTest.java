package com.murong.rpc.test;


import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.*;

import java.util.concurrent.atomic.AtomicLong;

public class ReconnectClientTest {

    public static void main(String[] args) throws Exception {
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8888);
        client.reConnect();

        AtomicLong atomicLong = new AtomicLong();
        ThreadUtil.run(30, () -> {
            long l1 = System.currentTimeMillis();
            while (true) {
                try {
                    StringBuilder sb = new StringBuilder("我");
                    RpcRequest rpcFileRequest = new RpcRequest();
                    rpcFileRequest.setBody(sb.toString());
                    long l = atomicLong.addAndGet(1l);
                    if (l % 10000 == 0) {
//                        RpcFuture rpcFuture = client.sendSynMsg(rpcFileRequest);
//                        RpcResponse rpcResponse = rpcFuture.get();
//                        System.out.println(rpcResponse.getBody());
                        long l2 = System.currentTimeMillis();
                        System.out.println(l * 1000 / (l2 - l1));
                    } else {
                        client.sendMsg(rpcFileRequest);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


}
