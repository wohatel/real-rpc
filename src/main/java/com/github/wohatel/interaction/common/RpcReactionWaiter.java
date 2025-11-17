package com.github.wohatel.interaction.common;


import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.file.RpcFileSenderInput;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;


/**
 * RpcReactionWaiter class - A utility class for handling RPC reactions and requests
 * over a network channel. It provides methods to send various types of RPC messages
 * and check the channel's active status.
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcReactionWaiter {

    @Getter
    private final ChannelHandlerContext ctx; // Channel handler context for network communication

    /**
     * Constructor for RpcReactionWaiter
     *
     * @param ctx The ChannelHandlerContext used for network communication
     */
    public RpcReactionWaiter(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Sends an RPC reaction to the remote peer
     *
     * @param rpcReaction The reaction object to be sent
     */
    public void sendReaction(RpcReaction rpcReaction) {
        RpcMsgTransManager.sendReaction(ctx.channel(), rpcReaction);
    }

    /**
     * Sends an RPC request to the remote peer
     *
     * @param request The request object to be sent
     */
    public void sendRequest(RpcRequest request) {
        RpcMsgTransManager.sendRequest(ctx.channel(), request);
    }

    /**
     * Sends a synchronous RPC request and returns a RpcFuture for response handling
     * @param request The request object to be sent
     * @return RpcFuture object for handling the response
     */
    public RpcFuture sendSynRequest(RpcRequest request) {
        return RpcMsgTransManager.sendSynRequest(ctx.channel(), request);
    }

    /**
     * Sends a synchronous RPC request with a specified timeout and returns a RpcFuture
     * @param rpcRequest The request object to be sent
     * @param timeOutMillis The timeout duration in milliseconds
     * @return RpcFuture object for handling the response
     */
    public RpcFuture sendSynRequest(RpcRequest rpcRequest, long timeOutMillis) {
        return RpcMsgTransManager.sendSynRequest(ctx.channel(), rpcRequest, timeOutMillis);
    }

    /**
     * Sends a file to the remote peer
     * @param file The file to be sent
     * @param input Additional parameters for file sending
     */
    public void sendFile(File file, RpcFileSenderInput input) {
        RpcMsgTransManager.sendFile(ctx.channel(), file, input);
    }

    /**
     * Checks if the channel is currently active
     * @return true if the channel exists and is active, false otherwise
     */
    public boolean isActive() {
        return ctx != null && ctx.channel().isActive();
    }
}
