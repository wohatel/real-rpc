package com.murong.rpc.interaction.handler;

import io.netty.channel.ChannelHandlerContext;

/**
 * 心跳链接响应超时处理
 *
 * @author yaochuang 2025/03/25 11:38
 */
public interface RpcHeartTimeOutHandler {
    void channelInactive(ChannelHandlerContext ctx);

}
