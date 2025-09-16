package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.bootstrap.ServerBootstrap;
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
public class RpcServer extends RpcDataReceiver {
    private final int port;
    private final MultiThreadIoEventLoopGroup group;
    private final MultiThreadIoEventLoopGroup childGroup;
    private final Class<? extends ServerChannel> serverChannelClass;

    public RpcServer(int port, MultiThreadIoEventLoopGroup group, MultiThreadIoEventLoopGroup childGroup) {
        super(false);
        this.port = port;
        this.group = group;
        this.childGroup = childGroup;
        serverChannelClass = getServerChannelClass();
    }

    /**
     * 开启nettyServer
     */
    @SneakyThrows
    public ChannelFuture start() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "RpcServer: 不要重复启动");
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
        throw new RpcException(RpcErrorEnum.CONNECT, "group 类型暂时不支持");
    }
}
