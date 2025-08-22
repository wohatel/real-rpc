package com.murong.rpc.client;

import com.murong.rpc.initializer.RpcMessageInteractionHandler;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.Channel;
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

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageClientInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMessageClientInteractionHandler.setRpcFileReceiverHandler(rpcFileReceiverHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestHandler) {
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
