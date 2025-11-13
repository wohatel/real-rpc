package com.github.wohatel.initializer;

import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.interaction.proxy.RpcFileChannelDataTransProxy;
import com.github.wohatel.interaction.proxy.RpcRequestChannelDataTransProxy;
import com.github.wohatel.interaction.proxy.RpcSessionChannelDataTransProxy;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author yaochuang 2025/06/30 17:33
 */
@Setter
@Getter
@ChannelHandler.Sharable
@Slf4j
@RequiredArgsConstructor
public class RpcMessageInteractionHandler extends ChannelInboundHandlerAdapter {
    private RpcFileRequestMsgHandler rpcFileRequestMsgHandler;
    private RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler;
    private RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMsg rpcMsg = (RpcMsg) msg;
        switch (rpcMsg.getRpcCommandType()) {
            case reaction -> RpcFutureTransManager.addReaction(rpcMsg.getPayload(RpcReaction.class));
            case request -> RpcRequestChannelDataTransProxy.channelRead(ctx, rpcMsg, rpcSimpleRequestMsgHandler);
            case session -> RpcSessionChannelDataTransProxy.channelRead(ctx, rpcMsg, rpcSessionRequestMsgHandler);
            case file -> RpcFileChannelDataTransProxy.channelRead(ctx, rpcMsg, rpcFileRequestMsgHandler);

            default -> {
            }
        }
    }
}
