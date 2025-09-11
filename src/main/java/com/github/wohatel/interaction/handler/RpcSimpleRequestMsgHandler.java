package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * 普通消息接收事件
 *
 * @author yaochuang 2025/03/25 11:29
 */
public interface RpcSimpleRequestMsgHandler {
    /**
     * 如果request请求要求有响应,response可以设置body,同时要执行写RpcMsgTransUtil.write操作
     * if request.isNeedResponse()
     * RpcMsgTransUtil.write(ctx.channel(), response);
     * 注意,如果操作特别耗时--- 需要用异步操作处理,避免线程阻塞,影响其它消息的消费
     *
     * @param ctx     channel上下文
     * @param request 请求
     */
    void channelRead(ChannelHandlerContext ctx, final RpcRequest request);
}
