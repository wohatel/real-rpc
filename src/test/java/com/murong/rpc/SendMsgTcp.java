package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.server.RpcServer;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendMsgTcp {


    public static final Map<Integer, String> MAP = new ConcurrentHashMap<>();

    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(4000);
        clientConnect();
    }

    public static void serverStart() {

        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            AtomicLong start = new AtomicLong(0L);
            rpcServer.setRpcSimpleRequestMsgHandler((ctx, req) -> {

                start.compareAndSet(0l, System.currentTimeMillis());

                String body = req.getBody();
                int length = body.length();
                RpcResponse response = req.toResponse();
                RpcMsgTransUtil.write(ctx.channel(), response);
            });
            rpcServer.start();


        });

    }

    public static void clientConnect() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
            defaultClient.connect();
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            TPSCounter tpsCounter = null;
            for (int i = 0; i <= 10; i++) {
                RpcRequest request = new RpcRequest();
                request.setBody(i + RandomStringUtils.randomAlphanumeric(20));
                RpcFuture rpcFuture = defaultClient.sendSynMsg(request);
                rpcFuture.get();
                if (i >= 1000) {
                    tpsCounter.increment();
                    if (i % 1000 == 0) {
                        System.out.println(tpsCounter.report());
                    }
                }
                if (i >= 1000) {
                    tpsCounter.increment();
                }
                if (i == 999) {
                    tpsCounter = new TPSCounter();
                }
            }
            System.out.println("最终结果:" + tpsCounter.report());

        });
    }

    public static Random random = new Random();

    public static String randomString() {
        int i = random.nextInt(300);
        StringBuilder stringBuilder = new StringBuilder();
        for (int j = 0; j < i; j++) {
            stringBuilder.append(j);
        }
        return stringBuilder.toString();
    }
}
