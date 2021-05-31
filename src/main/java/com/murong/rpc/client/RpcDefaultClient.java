package com.murong.rpc.client;


import com.murong.rpc.initializer.StringChannelInitializer;
import com.murong.rpc.interaction.RpcGc;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class RpcDefaultClient extends SimpleRpcClient {
    protected String host;
    protected Integer port;
    protected NioEventLoopGroup nioEventLoopGroup;

    public RpcDefaultClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        this.host = host;
        this.port = port;
        this.nioEventLoopGroup = nioEventLoopGroup;
    }

    public RpcDefaultClient(String host, int port) {
        this(host, port, new NioEventLoopGroup());
    }

    public ChannelFuture connect() {
        Bootstrap b = new Bootstrap();
        b.group(nioEventLoopGroup);
        b.channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
        b.handler(new StringChannelInitializer());
        ChannelFuture f = b.connect(host, port);
        f.addListener(new GenericFutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    setChannel(f.channel());
                }
            }
        });
        return f;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void closeNioEventLoopGroup() {
        if (this.nioEventLoopGroup != null) {
            this.nioEventLoopGroup.shutdownGracefully();
        }
    }

}
