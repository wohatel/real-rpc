package com.murong.rpc.client.handler;

import com.murong.rpc.interaction.RpcCommandType;
import com.murong.rpc.interaction.RpcInteractionContainer;
import com.murong.rpc.interaction.RpcMsg;
import com.murong.rpc.util.FileChannelUtil;
import com.murong.rpc.util.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class RpcMessageClientInteractionHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMsg rpcMsg = JsonUtil.parseObject(msg, RpcMsg.class);
        if (rpcMsg.getRpcCommandType() == RpcCommandType.response) {
            RpcInteractionContainer.addResponse(rpcMsg.getResponse());
        } else if (rpcMsg.getRpcCommandType() == RpcCommandType.request) {
            ctx.fireChannelRead(rpcMsg.getRequest());
        } else if (rpcMsg.getRpcCommandType() == RpcCommandType.file) {
            FileClientChannelHandler.readFileRequest(ctx, rpcMsg.getRpcFileRequest());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

}
