package com.github.wohatel.tcp;

import com.github.wohatel.initializer.RpcMessageInteractionHandler;
import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;


/**
 * @author yaochuang
 */
@Data
public class RpcDataReceiver {

    protected Channel channel;

    protected final RpcMsgChannelInitializer rpcMsgChannelInitializer = new RpcMsgChannelInitializer();

    protected final RpcMessageInteractionHandler rpcMessageInteractionHandler = new RpcMessageInteractionHandler();

    protected RpcDataReceiver() {
        rpcMsgChannelInitializer.getInitChannelHandlers().add(Pair.of("msgHandler", rpcMessageInteractionHandler));
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
