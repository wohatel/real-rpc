package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class AllTest {


    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(2000);
        clientConnect();
    }

    public static void serverStart() {

        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);
            rpcServer.setRpcSimpleRequestMsgHandler(new RpcSimpleRequestMsgHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                    System.out.println(request.getRequestId());
                    VirtualThreadPool.getEXECUTOR().execute(() -> {
                        System.out.println("我发送一个消息");
                        RpcRequest request1 = new RpcRequest();
                        request1.setBody("1");
                        RpcFuture rpcFuture = RpcMsgTransUtil.sendSynMsg(ctx.channel(), request1);
                    });

                    try {
                        // 举个例子，执行 ls -l
                        ProcessBuilder pb = new ProcessBuilder("bash", "-c", "ls -l");
                        pb.redirectErrorStream(true); // 合并错误输出流
                        Process process = pb.start();

                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream())
                        );

                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }

                        int exitCode = process.waitFor();
                        System.out.println("Exit code: " + exitCode);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
            });
            rpcServer.start();
        });

    }

    public static void clientConnect() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
            defaultClient.setRpcSimpleRequestMsgHandler(new RpcSimpleRequestMsgHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                    System.out.println("收到");
                }
            });
            defaultClient.connect();
            RpcRequest request = new RpcRequest();
            request.setBody("你好");
            defaultClient.sendMsg(request);

            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
