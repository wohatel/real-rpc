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
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcReactionWaiter {

    @Getter
    private final ChannelHandlerContext ctx;

    public RpcReactionWaiter(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public void sendReaction(RpcReaction rpcReaction) {
        RpcMsgTransManager.sendReaction(ctx.channel(), rpcReaction);
    }

    public void sendRequest(RpcRequest request) {
        RpcMsgTransManager.sendRequest(ctx.channel(), request);
    }

    public RpcFuture sendSynRequest(RpcRequest request) {
        return RpcMsgTransManager.sendSynRequest(ctx.channel(), request);
    }

    public RpcFuture sendSynRequest(RpcRequest rpcRequest, long timeOutMillis) {
        return RpcMsgTransManager.sendSynRequest(ctx.channel(), rpcRequest, timeOutMillis);
    }

    public void sendFile(File file, RpcFileSenderInput input) {
        RpcMsgTransManager.sendFile(ctx.channel(), file, input);
    }

    public boolean isActive() {
        return ctx != null && ctx.channel().isActive();
    }
}
