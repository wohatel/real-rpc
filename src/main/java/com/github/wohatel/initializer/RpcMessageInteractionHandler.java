package com.github.wohatel.initializer;

import com.github.wohatel.constant.RpcHeartAction;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
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
 * RPC message interaction handler for processing different types of RPC messages.
 *
 * @author yaochuang 2025/06/30 17:33
 */
@Setter
@Getter
@ChannelHandler.Sharable
@Slf4j
@RequiredArgsConstructor
public class RpcMessageInteractionHandler extends ChannelInboundHandlerAdapter {
    // Handler for RPC file request messages
    private RpcFileRequestMsgHandler rpcFileRequestMsgHandler;
    // Handler for RPC simple request messages
    private RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler;
    // Handler for RPC session request messages
    private RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler;

    /**
     * Processes incoming RPC messages based on their command type.
     *
     * @param ctx the ChannelHandlerContext which contains the Channel
     * @param msg the received message
     * @throws Exception if an error occurs during message processing
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Cast the incoming message to RpcMsg
        RpcMsg rpcMsg = (RpcMsg) msg;
        // Switch based on the RPC command type to route to appropriate handlers
        switch (rpcMsg.getRpcCommandType()) {
            // Handle reaction command type
            case reaction -> RpcFutureTransManager.addReaction(rpcMsg.getPayload(RpcReaction.class));
            // Handle request command type through simple request message handler
            case request -> RpcRequestChannelDataTransProxy.channelRead(ctx, rpcMsg, rpcSimpleRequestMsgHandler);
            // Handle session command type through session request message handler
            case session -> RpcSessionChannelDataTransProxy.channelRead(ctx, rpcMsg, rpcSessionRequestMsgHandler);
            // Handle file command type through file request message handler
            case file -> RpcFileChannelDataTransProxy.channelRead(ctx, rpcMsg, rpcFileRequestMsgHandler);

            case heart -> {
                RpcHeartAction payload = rpcMsg.getPayload(RpcHeartAction.class);
                if (RpcHeartAction.PING == payload) {
//                    RpcMsgTransManager.sendHeart(ctx.channel(), RpcHeartAction.PONG);
                }
            }
        }
    }
}
