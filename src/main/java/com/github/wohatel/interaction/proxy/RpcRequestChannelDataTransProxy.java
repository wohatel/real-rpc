package com.github.wohatel.interaction.proxy;

import com.github.wohatel.exception.RpcExceptionHandler;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.RpcReactionWaiter;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;



/**
 * A proxy class for handling RPC request channel data transmission.
 * This class provides a static method to process incoming RPC messages through a channel,
 * delegating the actual handling to a RpcSimpleRequestMsgHandler.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcRequestChannelDataTransProxy {

    /**
     * Processes an incoming RPC message from a channel.
     * This method extracts the RPC request from the message and passes it to the handler,
     * along with a reaction waiter for sending responses.
     * Exceptions are automatically caught and handled by wrapping the handler call.
     *
     * @param ctx The channel handler context, providing the connection information
     * @param rpcMsg The received RPC message containing the request
     * @param rpcSimpleRequestMsgHandler The handler that will process the request
     */
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        // Check if the handler is provided before processing
        if (rpcSimpleRequestMsgHandler != null) {
            try {
                // Extract the RPC request from the message payload
                RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
                RpcReactionWaiter waiter = new RpcReactionWaiter(ctx);
                // Delegate the request handling to the provided handler with a reaction waiter
                rpcSimpleRequestMsgHandler.onReceiveRequest(request, waiter);
            } catch (Throwable e) {
                // 统一异常处理：如果 handler 中抛出异常，自动发送错误响应
                RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
                RpcReactionWaiter waiter = new RpcReactionWaiter(ctx);
                RpcExceptionHandler.handleException(request, waiter, e);
            }
        }
    }

}