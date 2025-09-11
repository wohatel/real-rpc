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
public class RpcDataReceiver {

    protected Channel channel;

    protected final RpcMsgChannelInitializer rpcMsgChannelInitializer;

    protected final RpcMessageInteractionHandler rpcMessageInteractionHandler = new RpcMessageInteractionHandler();

    protected RpcDataReceiver() {
        this.rpcMsgChannelInitializer = new RpcMsgChannelInitializer(p -> p.addLast(rpcMessageInteractionHandler));
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMessageInteractionHandler.setRpcFileReceiverHandler(rpcFileReceiverHandler);
    }

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
    }

    public ChannelFuture close() {
        if (channel != null) {
            return channel.close();
        }
        return null;
    }
}
