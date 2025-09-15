package com.github.wohatel.tcp;

import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;


/**
 * @author yaochuang
 */
@Data
public class RpcDataReceiver {

    protected Channel channel;

    protected final RpcMsgChannelInitializer rpcMsgChannelInitializer = new RpcMsgChannelInitializer();

    protected RpcDataReceiver() {
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMsgChannelInitializer.onFileReceive(rpcFileReceiverHandler);
    }

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMsgChannelInitializer.onMsgReceive(rpcSimpleRequestMsgHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMsgChannelInitializer.onSessionMsgReceive(rpcSessionRequestMsgHandler);
    }

    public ChannelFuture close() {
        if (channel != null) {
            return channel.close();
        }
        return null;
    }
}
