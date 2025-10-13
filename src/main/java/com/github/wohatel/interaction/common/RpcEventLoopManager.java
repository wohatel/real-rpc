package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * description
 *
 * @author yaochuang 2025/10/11 16:49
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcEventLoopManager {
    private EventLoopGroup eventLoopGroup;
    private EventLoopGroup workerEventLoopGroup;
    private Class<? extends ServerChannel> serverChannelClass;
    private Class<? extends Channel> channelClass;
    private Class<? extends Channel> datagramChannelClass;

    private static RpcEventLoopManager of(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup, Class<? extends ServerChannel> serverChannelClass, Class<? extends Channel> channelClass, Class<? extends Channel> datagramChannelClass) {
        Objects.requireNonNull(eventLoopGroup);
        RpcEventLoopManager rpcEventLoopManager = new RpcEventLoopManager();
        rpcEventLoopManager.eventLoopGroup = eventLoopGroup;
        rpcEventLoopManager.workerEventLoopGroup = childEventLoopGroup;
        if (rpcEventLoopManager.workerEventLoopGroup == null) {
            rpcEventLoopManager.workerEventLoopGroup = eventLoopGroup;
        }
        rpcEventLoopManager.serverChannelClass = serverChannelClass;
        rpcEventLoopManager.channelClass = channelClass;
        rpcEventLoopManager.datagramChannelClass = datagramChannelClass;
        return rpcEventLoopManager;
    }

    public static RpcEventLoopManager ofServer(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup, Class<? extends ServerChannel> serverChannelClass) {
        return of(eventLoopGroup, childEventLoopGroup, serverChannelClass, null, null);
    }

    public static RpcEventLoopManager ofClient(EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass) {
        return of(eventLoopGroup, null, null, channelClass, null);
    }

    public static RpcEventLoopManager ofServer(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup) {
        return ofServer(eventLoopGroup, childEventLoopGroup, null);
    }

    public static RpcEventLoopManager of(EventLoopGroup eventLoopGroup) {
        return of(eventLoopGroup, null, null, null, null);
    }

    public static RpcEventLoopManager ofDefault() {
        return of(new NioEventLoopGroup());
    }

    public static RpcEventLoopManager ofUdp(EventLoopGroup eventLoopGroup, Class<? extends Channel> datagramChannelClass) {
        return of(eventLoopGroup, null, null, null, datagramChannelClass);
    }

    /**
     * 获取tcp server channelClass
     */
    public Class<? extends ServerChannel> getServerChannelClass() {
        if (serverChannelClass != null) {
            return serverChannelClass;
        }
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            return NioServerSocketChannel.class;
        }
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollServerSocketChannel.class;
        }
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            return KQueueServerSocketChannel.class;
        }
        throw new RpcException(RpcErrorEnum.CONNECT, "group types are not supported at the moment");
    }

    /**
     * 获取tcp client channelClass
     */
    public Class<? extends Channel> getChannelClass() {
        if (channelClass != null) {
            return channelClass;
        }
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            return NioSocketChannel.class;
        }
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollSocketChannel.class;
        }
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            return KQueueSocketChannel.class;
        }
        throw new RpcException(RpcErrorEnum.RUNTIME, "eventLoopGroup types are not supported at the moment");
    }

    /**
     * 获取udp channelClass
     */
    public Class<? extends Channel> getDatagramChannelClass() {
        if (this.datagramChannelClass != null) {
            return datagramChannelClass;
        }
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            return NioDatagramChannel.class;
        }
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollDatagramChannel.class;
        }
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            return KQueueDatagramChannel.class;
        }
        throw new RpcException(RpcErrorEnum.RUNTIME, "udp eventLoopGroup types are not supported");
    }

    public void shutdownGracefully() {
        if (eventLoopGroup != null) {
            if (!eventLoopGroup.isShutdown() && !eventLoopGroup.isShuttingDown()) {
                eventLoopGroup.shutdownGracefully();
            }
        }
        if (workerEventLoopGroup != null) {
            if (!workerEventLoopGroup.isShutdown() && !workerEventLoopGroup.isShuttingDown()) {
                workerEventLoopGroup.shutdownGracefully();
            }
        }
    }
}
