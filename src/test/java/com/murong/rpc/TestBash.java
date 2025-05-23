package com.murong.rpc;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.common.BashSession;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.TimeUnit;

/**
 * description
 *
 * @author yaochuang 2025/04/18 14:17
 */
public class TestBash {


    public static void main(String[] args) throws InterruptedException {
        serverStart();
        Thread.sleep(2000);
        clientConnect();
    }

    public static void serverStart() {

        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcServer rpcServer = new RpcServer(8765);

            final BashSession bash = new BashSession(System.out::println);
            rpcServer.setRpcSimpleRequestMsgHandler(new RpcSimpleRequestMsgHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                    String body = request.getBody();
                    System.out.println("收到了啊:" + body);
                    bash.sendCommand(body);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 20; i++) {
                        try {
                            String line = bash.getOutputQueue().poll(50, TimeUnit.MILLISECONDS);
                            if (line != null) {
                                sb.append(line + "\n");
                            }
                        } catch (InterruptedException e) {

                        }
                    }
                    RpcResponse response = request.toResponse();
                    response.setBody(sb.toString());
                }
            });
            rpcServer.setRpcSessionRequestMsgHandler(new RpcSessionRequestMsgHandler() {
                @Override
                public void sessionStart(ChannelHandlerContext ctx, RpcSession session, JSONObject context) {
                    System.out.println(session.getSessionId());
                    RpcMsgTransUtil.write(ctx.channel(), session.toResponse());
                    RpcMsgTransUtil.write(ctx.channel(), session.toResponse());
                    RpcMsgTransUtil.write(ctx.channel(), session.toResponse());
                }

                @Override
                public void channelRead(ChannelHandlerContext ctx, RpcSession session, JSONObject j, RpcSessionRequest request) {

                }

                @Override
                public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession, JSONObject context) {

                }

            });
            rpcServer.start();
        });

    }

    public static void clientConnect() {
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
            defaultClient.connect();
            RpcSession session = new RpcSession(NumberConstant.THREE_TEN_K);
            RpcSessionRequest request = new RpcSessionRequest(session);
            request.setBody("ls -al");
            RpcSessionFuture rpcSessionFuture = defaultClient.startSession(session, null);
            defaultClient.sendSessionMsg(request);
            request.setBody("cd /tmp");
            request.setBody("ls -al");
            defaultClient.sendSessionMsg(request);
            rpcSessionFuture.addListener(r -> {
                System.out.println("---结束--");
                String body = r.getBody();
                System.out.println(body);
            });
            System.out.println("发送");
            try {
                Thread.sleep(100000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
