package com.murong.rpc.client;

import com.murong.rpc.initializer.RpcMessageInteractionHandler;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

import java.io.Closeable;

/**
 * @author yaochuang
 */
@Data
public abstract class AbstractRpcClient implements Closeable {

    protected Channel channel;

    protected RpcMsgChannelInitializer rpcMsgChannelInitializer;

    protected final RpcMessageInteractionHandler rpcMessageClientInteractionHandler = new RpcMessageInteractionHandler(false);

    public void setRpcSimpleRequestMsgHandler(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageClientInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void setRpcFileRequestHandler(RpcFileRequestHandler rpcFileRequestHandler) {
        rpcMessageClientInteractionHandler.setRpcFileRequestHandler(rpcFileRequestHandler);
    }

    public void setRpcSessionRequestHandler(RpcSessionRequestMsgHandler rpcSessionRequestHandler) {
        rpcMessageClientInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestHandler);
    }

    @Override
    public void close() {
        if (channel != null) {
            channel.close();
        }
    }

    /**
     * 设置channel
     *
     * @param channel
     */
    protected void initClient(Channel channel) {
        this.channel = channel;
        this.channel.pipeline().addLast(rpcMessageClientInteractionHandler);
    }
}
