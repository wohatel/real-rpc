package com.github.wohatel.tcp;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
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

    public static String NODEID = System.currentTimeMillis() + NanoIdUtils.randomNanoId();

    protected Channel channel;

    protected final RpcMsgChannelInitializer rpcMsgChannelInitializer = new RpcMsgChannelInitializer();

    protected final boolean client;

    protected RpcDataReceiver(boolean client) {
        this.client = client;
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMsgChannelInitializer.onFileReceive(rpcFileReceiverHandler);
    }

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMsgChannelInitializer.onMsgReceive(rpcSimpleRequestMsgHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        if (client) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "client端暂不支持session请求处理");
        }
        rpcMsgChannelInitializer.onSessionMsgReceive(rpcSessionRequestMsgHandler);
    }

    public ChannelFuture close() {
        if (channel != null) {
            return channel.close();
        }
        return null;
    }
}
