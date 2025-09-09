package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;
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

    /**
     * 测试同步执行相应的tps -- 也就是说1s钟,发送+响应;一共多少次;
     * 正常情况下
     * 阻塞的是4-5万的tps  -- 发送后等待响应为1次
     * 非阻塞的是20万的tps -- 发送后不等待响应,由异步线程处理响应
     *
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(4000);
        clientConnect();
    }

    public static void serverStart() {

        VirtualThreadPool.execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            AtomicLong start = new AtomicLong(0L);
            rpcServer.onMsgReceive((ctx, req) -> {

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
        VirtualThreadPool.execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
            defaultClient.connect();
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            TPSCounter tpsCounter = new TPSCounter();
            for (int i = 0; i <= 1000000; i++) {
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
