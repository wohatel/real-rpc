package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;


/**
 * A manager class for handling socket-based RPC events using EventLoopGroup.
 * This class extends RpcEventLoopManager and provides functionality to manage
 * different types of channels based on the EventLoopGroup implementation.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSocketEventLoopManager extends RpcEventLoopManager<Channel> {

    /**
     * Creates a new RpcSocketEventLoopManager instance with the specified EventLoopGroup and Channel class.
     * This is a static factory method that provides a convenient way to create and configure an RpcSocketEventLoopManager.
     *
     * @param eventLoopGroup the EventLoopGroup to be used by the manager
     * @param channelClass   the class of the Channel to be used by the manager
     * @return a new RpcSocketEventLoopManager instance configured with the provided parameters
     * @throws NullPointerException if eventLoopGroup is null
     */
    public static RpcSocketEventLoopManager of(EventLoopGroup eventLoopGroup, Class<Channel> channelClass) {
        // Validate that eventLoopGroup is not null
        Objects.requireNonNull(eventLoopGroup);
        // Create a new RpcSocketEventLoopManager instance
        RpcSocketEventLoopManager eventLoopGroupManager = new RpcSocketEventLoopManager();
        // Configure the eventLoopGroup and channelClass for the manager
        eventLoopGroupManager.eventLoopGroup = eventLoopGroup;
        eventLoopGroupManager.channelClass = channelClass;
        // Return the configured manager
        return eventLoopGroupManager;
    }

    public static RpcSocketEventLoopManager of(EventLoopGroup eventLoopGroup) {
        return of(eventLoopGroup, null);
    }

    public static RpcSocketEventLoopManager of() {
        return of(new NioEventLoopGroup());
    }

    public Class<? extends Channel> getChannelClass() {
        // If channelClass has already been set, return it directly
        if (channelClass != null) {
            return channelClass;
        }
        // Check if the eventLoopGroup is of type NioEventLoopGroup
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            // Use NIO socket channel for NIO event loop group
            return NioSocketChannel.class;
        }
        // Check if the eventLoopGroup is of type EpollEventLoopGroup
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            // Use Epoll socket channel for Epoll event loop group (Linux specific)
            return EpollSocketChannel.class;
        }
        // Check if the eventLoopGroup is of type KQueueEventLoopGroup
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            // Use KQueue socket channel for KQueue event loop group (BSD/macOS specific)
            return KQueueSocketChannel.class;
        }
        // Throw an exception if none of the supported eventLoopGroup types match
        throw new RpcException(RpcErrorEnum.RUNTIME, "eventLoopGroup types are not supported at the moment");
    }
}
