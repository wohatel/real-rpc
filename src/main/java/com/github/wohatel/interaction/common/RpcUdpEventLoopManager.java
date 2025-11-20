package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;



/**
 * A manager class for UDP-based RPC event loops, extending the base RpcEventLoopManager.
 * This class provides factory methods to create instances and manages the appropriate DatagramChannel
 * based on the type of EventLoopGroup provided.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcUdpEventLoopManager extends RpcEventLoopManager<DatagramChannel> {

    /**
     * Factory method to create an instance of RpcUdpEventLoopManager with specified EventLoopGroup and channel class.
     *
     * @param eventLoopGroup The EventLoopGroup to be used
     * @param channelClass   The class of DatagramChannel to be used
     * @return A new instance of RpcUdpEventLoopManager
     * @throws NullPointerException if eventLoopGroup is null
     */
    public static RpcUdpEventLoopManager of(EventLoopGroup eventLoopGroup, Class<DatagramChannel> channelClass) {
        Objects.requireNonNull(eventLoopGroup);
        RpcUdpEventLoopManager eventLoopGroupManager = new RpcUdpEventLoopManager();
        eventLoopGroupManager.eventLoopGroup = eventLoopGroup;
        eventLoopGroupManager.channelClass = channelClass;
        return eventLoopGroupManager;
    }

    /**
     * Factory method to create an instance of RpcUdpEventLoopManager with specified EventLoopGroup.
     * The channel class will be determined automatically based on the EventLoopGroup type.
     *
     * @param eventLoopGroup The EventLoopGroup to be used
     * @return A new instance of RpcUdpEventLoopManager
     */
    public static RpcUdpEventLoopManager of(EventLoopGroup eventLoopGroup) {
        return of(eventLoopGroup, null);
    /**
     * Factory method to create an instance of RpcUdpEventLoopManager with a default NioEventLoopGroup.
     * The channel class will be determined automatically based on the EventLoopGroup type.
     * @return A new instance of RpcUdpEventLoopManager with a default NioEventLoopGroup
     */
    }

    public static RpcUdpEventLoopManager of() {
        return of(new NioEventLoopGroup());
    /**
     * Returns the appropriate DatagramChannel class based on the configured EventLoopGroup.
     * If channelClass has been explicitly set, it will be returned. Otherwise, the method will
     * determine the appropriate channel class based on the type of EventLoopGroup.
     * @return The class of DatagramChannel to be used
     * @throws RpcException if the EventLoopGroup type is not supported
     */
    }

    public Class<? extends DatagramChannel> getChannelClass() {
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
}
