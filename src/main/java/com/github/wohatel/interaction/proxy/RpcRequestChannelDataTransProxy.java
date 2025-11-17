package com.github.wohatel.interaction.proxy;

import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.RpcReactionWaiter;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
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
     *
     * @param ctx The channel handler context, providing the connection information
     * @param rpcMsg The received RPC message containing the request
     * @param rpcSimpleRequestMsgHandler The handler that will process the request
     * @throws Exception If any error occurs during processing (handled by @SneakyThrows)
     */
    @SneakyThrows
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        // Check if the handler is provided before processing
        if (rpcSimpleRequestMsgHandler != null) {
            // Extract the RPC request from the message payload
            RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
            // Delegate the request handling to the provided handler with a reaction waiter
            rpcSimpleRequestMsgHandler.onReceiveRequest(request, new RpcReactionWaiter(ctx));
        }
    }

}