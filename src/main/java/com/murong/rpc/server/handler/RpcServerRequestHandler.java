package com.murong.rpc.server.handler;

import com.murong.rpc.interaction.RpcMsgTransUtil;
import com.murong.rpc.interaction.RpcRequest;
import com.murong.rpc.interaction.RpcResponse;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


@ChannelHandler.Sharable
public class RpcServerRequestHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcRequest request = (RpcRequest) msg;
        System.out.println(request.getBody());
        if (request.isNeedResponse()){
            RpcResponse rpcResponse = request.toResponse();
            rpcResponse.setBody("收到了");
            RpcMsgTransUtil.write(ctx.channel(), rpcResponse);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }
}
