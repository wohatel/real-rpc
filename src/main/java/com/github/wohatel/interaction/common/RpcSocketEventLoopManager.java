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

    public static RpcSocketEventLoopManager of(EventLoopGroup eventLoopGroup, Class<Channel> channelClass) {
    /**
     * Factory method to create an instance of RpcSocketEventLoopManager with specified EventLoopGroup and Channel class.
     *
     * @param eventLoopGroup The EventLoopGroup to be used
     * @param channelClass The specific Channel class to be used
     * @return A new instance of RpcSocketEventLoopManager
     * @throws NullPointerException if eventLoopGroup is null
     */
        Objects.requireNonNull(eventLoopGroup);
        RpcSocketEventLoopManager eventLoopGroupManager = new RpcSocketEventLoopManager();
        eventLoopGroupManager.eventLoopGroup = eventLoopGroup;
        eventLoopGroupManager.channelClass = channelClass;
        return eventLoopGroupManager;
    }

    public static RpcSocketEventLoopManager of(EventLoopGroup eventLoopGroup) {
    /**
     * Factory method to create an instance of RpcSocketEventLoopManager with specified EventLoopGroup.
     * Uses default Channel class which will be determined based on EventLoopGroup type.
     *
     * @param eventLoopGroup The EventLoopGroup to be used
     * @return A new instance of RpcSocketEventLoopManager
     */
        return of(eventLoopGroup, null);
    }

    public static RpcSocketEventLoopManager of() {
    /**
     * Factory method to create an instance of RpcSocketEventLoopManager with default NioEventLoopGroup.
     * Uses NIO socket channel by default.
     *
     * @return A new instance of RpcSocketEventLoopManager
     */
        return of(new NioEventLoopGroup());
    }

    public Class<? extends Channel> getChannelClass() {
    /**
     * Returns the appropriate Channel class based on the EventLoopGroup type.
     * If channelClass has already been set, it returns that directly.
     * Otherwise, it determines the appropriate Channel class based on the type of EventLoopGroup.
     *
     * @return The appropriate Channel class
     * @throws RpcException if the EventLoopGroup type is not supported
     */
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
