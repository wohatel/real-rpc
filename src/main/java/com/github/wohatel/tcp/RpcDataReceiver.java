package com.github.wohatel.tcp;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.Getter;


@Data
public class RpcDataReceiver {

    protected final String uniqueId;

    protected final String host;

    protected final int port;

    @Getter
    protected Channel channel;

    protected RpcMsgChannelInitializer rpcMsgChannelInitializer = new RpcMsgChannelInitializer();

    protected RpcDataReceiver(String host, int port) {
        this.uniqueId = NanoIdUtils.randomNanoId();
        this.host = host;
        this.port = port;
    }

    protected RpcDataReceiver(Integer port) {
        this(null, port);
    }


    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMsgChannelInitializer.onFileReceive(rpcFileReceiverHandler);
    }

    public void onRequestReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMsgChannelInitializer.onRequestReceive(rpcSimpleRequestMsgHandler);
    }

    public void onSessionRequestReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMsgChannelInitializer.onSessionRequestReceive(rpcSessionRequestMsgHandler);
    }

    public ChannelFuture close() {
        if (channel != null) {
            return channel.close();
        }
        return null;
    }
}
