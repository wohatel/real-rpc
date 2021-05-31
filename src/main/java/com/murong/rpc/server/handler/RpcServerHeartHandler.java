/*
 * Copyright (c) 2017 Beijing Tiande Technology Co., Ltd.
 * All Rights Reserved.
 */
package com.murong.rpc.server.handler;

import com.murong.rpc.interaction.RpcMsgTransUtil;
import com.murong.rpc.interaction.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * TCP message handler.
 *
 * @author murong 2018-08-03
 * @version 1.0
 */
public class RpcServerHeartHandler extends ChannelInboundHandlerAdapter {

    private long nearTime = System.currentTimeMillis();
    private boolean active = true;
    private long timeOut = 90000l;

    public RpcServerHeartHandler() {

    }

    public RpcServerHeartHandler(long timeOut) {
        this.timeOut = timeOut;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        nearTime = System.currentTimeMillis();
        System.out.println("服务端收到心跳:" + msg);
        RpcMsgTransUtil.write(ctx.channel(), new RpcResponse());
    }

    public boolean isTimeOut() {
        // 如果连接断开,则肯定超时
        if (active == false) {
            return true;
        }
        long l = System.currentTimeMillis() - nearTime;
        if (l > timeOut) {
            return true;
        }
        return false;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext var1) throws Exception {
        this.active = false;
    }
}
