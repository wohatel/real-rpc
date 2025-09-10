package com.murong.rpc.tcp;

import com.murong.rpc.initializer.RpcMessageInteractionHandler;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;


/**
 * @author yaochuang
 */
@Data
public abstract class AbstractRpcClient {

    protected Channel channel;

    protected RpcMsgChannelInitializer rpcMsgChannelInitializer;

    protected final RpcMessageInteractionHandler rpcMessageClientInteractionHandler = new RpcMessageInteractionHandler();

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageClientInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMessageClientInteractionHandler.setRpcFileReceiverHandler(rpcFileReceiverHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestHandler) {
        rpcMessageClientInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestHandler);
    }

    public ChannelFuture close() {
        if (channel != null) {
            return channel.close();
        }
        return null;
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
