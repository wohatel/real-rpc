package com.murong.rpc.interaction.handler;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionRequest;
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
    void sessionStart(ChannelHandlerContext ctx, final RpcSession rpcSession);

    /**
     * 注意,一旦处理消息较为耗时,会影响其它消息的消费,建议使用异步线程处理读取逻辑
     *
     * @param ctx
     * @param request
     */
    void channelRead(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionRequest request);

    /**
     * 接收到对方发来的结束会话请求
     *
     * @param rpcSession 会话
     */
    void sessionStop(ChannelHandlerContext ctx, final RpcSession rpcSession);

}
