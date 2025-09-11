package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;

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
        RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
        rpcServer.onSessionMsgReceive(new RpcSessionRequestMsgHandler() {
            @Override
            public void sessionStart(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                System.out.println("你传入了:" + context.getTopic());
                new Thread(() -> {
                    // 主题是什么
                    RpcResponse response = rpcSession.toResponse();
                    response.setBody("db");
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }).start();
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request, RpcSessionContext context) {
                // 这是要干嘛
                System.out.println("read:" + request.getBody());
                RpcResponse response = rpcSession.toResponse();
                response.setBody("收到");
                RpcMsgTransUtil.write(ctx.channel(), response);
            }

            @Override
            public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                System.out.println("结束:");
            }
        });
        rpcServer.start();
    }


}
