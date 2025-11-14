package com.github.wohatel.tcp;

import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.util.RandomUtil;
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
        this.uniqueId = RandomUtil.randomUUIDWithTime();
        this.host = host;
        this.port = port;
    }

    protected RpcDataReceiver(Integer port) {
        this(null, port);
    }


    public void onFileReceive(RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        rpcMsgChannelInitializer.onFileReceive(rpcFileRequestMsgHandler);
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
