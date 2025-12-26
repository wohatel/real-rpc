package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcHeartAction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public abstract class RpcHeartHandler extends IdleStateHandler {

    public RpcHeartHandler(long readerIdleMs, long writerIdleMs) {
        super(readerIdleMs, writerIdleMs, 0, TimeUnit.MILLISECONDS);
    }

    public abstract void onChannelHeatTimeOut(ChannelHandlerContext ctx) throws Exception;

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        // 这里直接处理心跳，不需要传给下一个 handler
        switch (evt.state()) {
            case WRITER_IDLE:
                RpcMsgTransManager.sendHeart(ctx.channel(), RpcHeartAction.PING);
                break;
            case READER_IDLE:
                this.onChannelHeatTimeOut(ctx);
                break;
        }
        ctx.fireUserEventTriggered(evt);
    }

}