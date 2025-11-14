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
 * @author yaochuang
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcRequestChannelDataTransProxy {

    @SneakyThrows
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        if (rpcSimpleRequestMsgHandler != null) {
            RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
            rpcSimpleRequestMsgHandler.onReceiveRequest(request, new RpcReactionWaiter(ctx));
        }
    }

}