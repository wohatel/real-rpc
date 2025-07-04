package com.murong.rpc.server;

import com.murong.rpc.initializer.RpcMessageInteractionHandler;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.net.InetSocketAddress;

/**
 * 绑定服务器到监听的端口，配置Channel，将入站消息通知给EchoServerHandler实例
 *
 * @version 1.0.0
 * @since 2021/5/25上午12:55
 */
@Log
public class RpcServer implements AutoCloseable {

    @Getter
    private Channel channel;
    private final int port;
    private final EventLoopGroup group;
    private final EventLoopGroup childGroup;
    private final RpcMsgChannelInitializer rpcMsgChannelInitializer;

    private final RpcMessageInteractionHandler rpcMessageServerInteractionHandler = new RpcMessageInteractionHandler(true);

    public RpcServer(int port, EventLoopGroup group, EventLoopGroup childGroup) {
        this.port = port;
        this.group = group;
        this.childGroup = childGroup;
        this.rpcMsgChannelInitializer = new RpcMsgChannelInitializer(p -> p.addLast(rpcMessageServerInteractionHandler));
    }


    public RpcServer(int port) {
        this(port, new NioEventLoopGroup(), new NioEventLoopGroup());
    }


    public void setRpcFileRequestHandler(RpcFileRequestHandler rpcFileRequestHandler) {
        rpcMessageServerInteractionHandler.setRpcFileRequestHandler(rpcFileRequestHandler);
    }

    public void setRpcSimpleRequestMsgHandler(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageServerInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void setRpcSessionRequestMsgHandler(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMessageServerInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
    }

    /**
     * 开启nettyServer
     */
    @SneakyThrows
    public ChannelFuture start() {
        if (this.channel != null) {
            throw new RuntimeException("RpcServer: 不要重复启动");
        }
        ServerBootstrap b = new ServerBootstrap();
        b.group(group, childGroup).channel(NioServerSocketChannel.class).localAddress(new InetSocketAddress(port)).childHandler(rpcMsgChannelInitializer);
        ChannelFuture future = b.bind().sync().addListener(futureListener -> {
            if (!futureListener.isSuccess()) {
                Throwable cause = futureListener.cause();
                log.warning("Netty 服务启动失败，原因：" + cause);
            }
        });
        this.channel = future.channel(); // 用于关闭server
        future.channel().closeFuture().addListener(futureListener -> {
            if (futureListener.isSuccess()) {
                group.shutdownGracefully();
                childGroup.shutdownGracefully();
                if (!futureListener.isSuccess()) {
                    log.warning("Netty 服务关闭异常：" + futureListener.cause());
                } else {
                    log.info("Netty 服务已关闭");
                }
            }
        });
        return future;
    }

    @Override
    public void close() {
        if (this.channel != null) {
            this.channel.close();
        }
    }

}
