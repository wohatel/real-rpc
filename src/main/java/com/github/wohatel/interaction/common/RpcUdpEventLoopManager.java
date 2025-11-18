package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Data;

import java.util.Objects;


/**
 * A manager class for RPC event loops that handles different types of event loop groups and their associated channel classes.
 * This class provides factory methods to create instances for server, client, and UDP configurations.
 */
@Data
public class RpcUdpEventLoopManager {
    // Event loop group for accepting connections
    private EventLoopGroup eventLoopGroup;
    // Class for client channel implementation
    private Class<? extends Channel> channelClass;

    public RpcUdpEventLoopManager(EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass) {
        Objects.requireNonNull(eventLoopGroup);
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = channelClass;
    }

    public RpcUdpEventLoopManager(EventLoopGroup eventLoopGroup) {
        this(eventLoopGroup, null);
    }

    public RpcUdpEventLoopManager() {
        this(new NioEventLoopGroup());
    }

    public Class<? extends Channel> getChannelClass() {
        // If channelClass has already been set, return it directly
        if (channelClass != null) {
            return channelClass;
        }
        // Check if the eventLoopGroup is of type NioEventLoopGroup
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            // Use NIO socket channel for NIO event loop group
            return NioDatagramChannel.class;
        }
        // Check if the eventLoopGroup is of type EpollEventLoopGroup
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            // Use Epoll socket channel for Epoll event loop group (Linux specific)
            return EpollDatagramChannel.class;
        }
        // Check if the eventLoopGroup is of type KQueueEventLoopGroup
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            // Use KQueue socket channel for KQueue event loop group (BSD/macOS specific)
            return KQueueDatagramChannel.class;
        }
        // Throw an exception if none of the supported eventLoopGroup types match
        throw new RpcException(RpcErrorEnum.RUNTIME, "eventLoopGroup types are not supported at the moment");
    }

    public Class<? extends Channel> channelClass() {
        // If the datagramChannelClass is already set, return it directly
        if (this.channelClass != null) {
            return channelClass;
        }
        // Check if the EventLoopGroup is NIO-based and return NioDatagramChannel
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            return NioDatagramChannel.class;
        }
        // Check if the EventLoopGroup is Epoll-based and return EpollDatagramChannel
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollDatagramChannel.class;
        }
        // Check if the EventLoopGroup is KQueue-based and return KQueueDatagramChannel
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            return KQueueDatagramChannel.class;
        }
        // If none of the supported EventLoopGroup types match, throw an exception
        throw new RpcException(RpcErrorEnum.RUNTIME, "udp eventLoopGroup types are not supported");
    }

    /**
     * Gracefully shuts down the event loop groups if they are not already shutting down or shut down.
     * This method ensures a clean termination of all active I/O operations and releases resources.
     */
    public void shutdownGracefully() {
        // Check if the main event loop group is not null and not already in shutdown process
        if (eventLoopGroup != null && (!eventLoopGroup.isShutdown() && !eventLoopGroup.isShuttingDown())) {
            // Shutdown the main event loop group gracefully
            eventLoopGroup.shutdownGracefully();
        }
    }


}
