package com.murong.rpc.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;

public class RpcAutoReconnectClient extends RpcDefaultClient {

    /**
     * 是否允许自动重连
     */
    private boolean allowAutoConnect = true;

    public RpcAutoReconnectClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, nioEventLoopGroup);
    }

    public RpcAutoReconnectClient(String host, int port) {
        this(host, port, new NioEventLoopGroup());
    }


    public void reConnect() {
        if (!allowAutoConnect) {
            return; // 销毁后,禁止重连
        }
        ChannelFuture future = this.connect();
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture futureListener) throws Exception {
                if (futureListener.isSuccess()) {
//                    channel = futureListener.channel();
                    channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                            reConnect();
                        }
                    });
                } else {
                    futureListener.channel().eventLoop().schedule(() -> {
                        try {
                            reConnect();
                        } catch (Exception e) {
                        }
                    }, 5, TimeUnit.SECONDS);
                }
            }
        });

    }


    public boolean isAllowAutoConnect() {
        return allowAutoConnect;
    }

    public void setAllowAutoConnect(boolean allowAutoConnect) {
        this.allowAutoConnect = allowAutoConnect;
    }
}
