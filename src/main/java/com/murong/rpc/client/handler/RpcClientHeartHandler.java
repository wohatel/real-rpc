/*
 * Copyright (c) 2017 Beijing Tiande Technology Co., Ltd.
 * All Rights Reserved.
 */
package com.murong.rpc.client.handler;


import com.murong.rpc.client.SimpleRpcClient;
import com.murong.rpc.interaction.RpcMsgTransUtil;
import com.murong.rpc.interaction.RpcRequest;
import com.murong.rpc.interaction.RpcRequestType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TCP message handler.
 *
 * @author murong 2018-08-03
 * @version 1.0
 */
public class RpcClientHeartHandler extends ChannelInboundHandlerAdapter {

    private SimpleRpcClient simpleRpcClient;

    public RpcClientHeartHandler(SimpleRpcClient simpleRpcClient) {
        this.simpleRpcClient = simpleRpcClient;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        IdleStateEvent idle = (IdleStateEvent) evt;
        if (idle.state() == IdleState.WRITER_IDLE) {
            System.out.println("写一次");
            RpcRequest request = new RpcRequest();
            request.setRequestType(RpcRequestType.heart.name());
            RpcMsgTransUtil.sendMsg(ctx.channel(), request);
        } else if (idle.state() == IdleState.READER_IDLE) {
            // 此时说明心跳已经超时
            System.out.println("服务端超时超时超时");
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
    }

    @Autowired
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

}
