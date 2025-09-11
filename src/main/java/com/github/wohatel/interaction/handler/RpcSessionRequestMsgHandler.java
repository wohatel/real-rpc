package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcSessionContext;
import io.netty.channel.ChannelHandlerContext;

/**
 * session消息接收方处理消息事件
 *
 * @author yaochuang 2025/03/25 11:29
 */
public interface RpcSessionRequestMsgHandler {
    /**
     * 注意,一旦处理消息较为耗时,会影响其它消息的消费,建议使用异步线程处理读取逻辑
     *
     * @param ctx
     * @param rpcSession
     */
    default void sessionStart(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context) {

    }

    /**
     * 注意,一旦处理消息较为耗时,会影响其它消息的消费,建议使用异步线程处理读取逻辑
     *
     * @param ctx
     * @param request
     */
    void channelRead(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionRequest request, final RpcSessionContext context);

    /**
     * 接收到对方发来的结束会话请求
     *
     * @param rpcSession 会话
     */
    default void sessionStop(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context) {

    }

}
