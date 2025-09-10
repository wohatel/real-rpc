package com.murong.rpc.tcp;

import com.murong.rpc.initializer.RpcMessageInteractionHandler;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 绑定服务器到监听的端口，配置Channel，将入站消息通知给EchoServerHandler实例
 *
 * @author yaochuang
 * @version 1.0.0
 * @since 2021/5/25上午12:55
 */
@Getter
@Slf4j
public class RpcServer {

    private Channel channel;
    private final int port;
    private final MultiThreadIoEventLoopGroup group;
    private final MultiThreadIoEventLoopGroup childGroup;
    @Setter
    private RpcMsgChannelInitializer rpcMsgChannelInitializer;
    private final Class<? extends ServerChannel> serverChannelClass;

    private final RpcMessageInteractionHandler rpcMessageServerInteractionHandler = new RpcMessageInteractionHandler();

    public RpcServer(int port, MultiThreadIoEventLoopGroup group, MultiThreadIoEventLoopGroup childGroup) {
        this.port = port;
        this.group = group;
        this.childGroup = childGroup;
        serverChannelClass = getServerChannelClass();
        this.rpcMsgChannelInitializer = new RpcMsgChannelInitializer(p -> p.addLast(rpcMessageServerInteractionHandler));
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMessageServerInteractionHandler.setRpcFileReceiverHandler(rpcFileReceiverHandler);
    }

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageServerInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMessageServerInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
    }

    /**
     * 开启nettyServer
     */
    @SneakyThrows
    public ChannelFuture start() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RuntimeException("RpcServer: 不要重复启动");
        }
        ServerBootstrap b = new ServerBootstrap();
        b.group(group, childGroup).channel(serverChannelClass).localAddress(new InetSocketAddress(port)).childHandler(rpcMsgChannelInitializer);
        ChannelFuture future = b.bind().addListener(futureListener -> {
            if (!futureListener.isSuccess()) {
                Throwable cause = futureListener.cause();
                log.error("Netty 服务启动失败，原因：", cause);
            }
        });
        this.channel = future.channel(); // 用于关闭server
        return future;
    }

    public ChannelFuture close() {
        if (this.channel != null) {
            return this.channel.close();
        }
        return null;
    }

    /**
     * 返回类型
     */
    protected Class<? extends ServerChannel> getServerChannelClass() {
        if (this.group.isIoType(NioIoHandler.class)) {
            return NioServerSocketChannel.class;
        }
        if (this.group.isIoType(EpollIoHandler.class)) {
            return EpollServerSocketChannel.class;
        }
        if (this.group.isIoType(KQueueIoHandler.class)) {
            return KQueueServerSocketChannel.class;
        }
        if (this.group.isIoType(LocalIoHandler.class)) {
            return LocalServerChannel.class;
        }
        throw new RuntimeException("group 类型暂时不支持");
    }
}
