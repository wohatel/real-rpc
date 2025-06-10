package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.interaction.handler.RpcResponseMsgListener;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendSessionMsgServer {


    public static void main(String[] args) throws InterruptedException {
        serverStart();
    }

    /**
     * 发送消息测试
     */
    public static void serverStart() {
        RpcServer rpcServer = new RpcServer(8765);
        rpcServer.setRpcSessionRequestMsgHandler(new RpcSessionRequestMsgHandler() {
            @Override
            public void sessionStart(ChannelHandlerContext ctx, RpcSession rpcSession) {
                new Thread(() -> {
                    // 主题是什么
                    System.out.println(rpcSession.getTopic() + ":" + rpcSession.getSessionId());
                    RpcResponse response = rpcSession.toResponse();
                    response.setBody("db");
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }).start();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request) {
                // 这是要干嘛
                System.out.println("read:" + request.getBody());
            }

            @Override
            public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession) {
                System.out.println("结束:");
            }
        });
        rpcServer.start();
    }


}
