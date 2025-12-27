package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcHeartAction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public abstract class RpcVivoHandler extends IdleStateHandler {

    public RpcVivoHandler(long readerIdleMills, long writerIdleMills) {
        super(readerIdleMills, writerIdleMills, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public final void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.onChannelActive(ctx);
    }

    @Override
    public final void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.onChannelInactive(ctx);
    }

    public abstract void onChannelHeatTimeOut(ChannelHandlerContext ctx);

    public abstract void onChannelActive(ChannelHandlerContext ctx);

    public abstract void onChannelInactive(ChannelHandlerContext ctx);

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