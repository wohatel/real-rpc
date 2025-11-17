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
 * A manager class for RPC event loops that handles different types of event loop groups and their associated channel classes.
 * This class provides factory methods to create instances for server, client, and UDP configurations.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcEventLoopManager {
    // Event loop group for accepting connections
    private EventLoopGroup eventLoopGroup;
    // Worker event loop group for handling incoming connections
    private EventLoopGroup workerEventLoopGroup;
    // Class for server channel implementation
    private Class<? extends ServerChannel> serverChannelClass;
    // Class for client channel implementation
    private Class<? extends Channel> channelClass;
    // Class for datagram channel implementation
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

    /**
     * Creates a server-side RpcEventLoopManager with the specified event loops and channel class.
     *
     * @param eventLoopGroup      The main event loop group for handling server connections
     * @param childEventLoopGroup The child event loop group for handling incoming connections
     * @param serverChannelClass  The class of the server channel to be used
     * @return A new RpcEventLoopManager instance configured for server operations
     */
    public static RpcEventLoopManager ofServer(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup, Class<? extends ServerChannel> serverChannelClass) {
        // Create and return a new RpcEventLoopManager using the provided parameters
        return of(eventLoopGroup, childEventLoopGroup, serverChannelClass, null, null);
    }

    /**
     * Creates a client-side RpcEventLoopManager with the specified event loop group and channel class.
     * This is a factory method that provides a simplified way to create a client-side event loop manager.
     *
     * @param eventLoopGroup The event loop group to be used for I/O operations
     * @param channelClass   The class of the channel to be used (e.g., NioSocketChannel)
     * @return A new RpcEventLoopManager instance configured for client use
     */
    public static RpcEventLoopManager ofClient(EventLoopGroup eventLoopGroup, Class<? extends Channel> channelClass) {
        // Delegate to the main of method with null values for server-specific parameters
        return of(eventLoopGroup, null, null, channelClass, null);
    }

    /**
     * Creates a server-side RpcEventLoopManager with the specified event loop groups.
     * This is a convenience method that creates an RpcEventLoopManager with default configuration.
     *
     * @param eventLoopGroup      The main event loop group for accepting new connections
     * @param childEventLoopGroup The event loop group for handling the accepted connections
     * @return A new RpcEventLoopManager instance configured for server use
     */
    public static RpcEventLoopManager ofServer(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup) {
        // Delegate to the main ofServer method with null as the third parameter
        return ofServer(eventLoopGroup, childEventLoopGroup, null);
    }

    /**
     * Static factory method to create an RpcEventLoopManager with default values
     *
     * @param eventLoopGroup The EventLoopGroup to be used by the RpcEventLoopManager
     * @return A new RpcEventLoopManager instance configured with the provided EventLoopGroup
     * and default values for other parameters
     */
    public static RpcEventLoopManager of(EventLoopGroup eventLoopGroup) {
        // Delegate to the main factory method with null values for unspecified parameters
        return of(eventLoopGroup, null, null, null, null);
    }

    /**
     * Creates a default RpcEventLoopManager instance using NioEventLoopGroup.
     * This is a factory method that provides a convenient way to create a manager with default NIO implementation.
     *
     * @return a new RpcEventLoopManager instance configured with default NioEventLoopGroup
     */
    public static RpcEventLoopManager ofDefault() {
        // Create a new RpcEventLoopManager with a default NioEventLoopGroup
        return of(new NioEventLoopGroup());
    }

    /**
     * Creates a new RpcEventLoopManager instance configured for UDP communication.
     *
     * @param eventLoopGroup       The event loop group to use for I/O operations
     * @param datagramChannelClass The class of the datagram channel to be used
     * @return A new RpcEventLoopManager instance configured for UDP
     */
    public static RpcEventLoopManager ofUdp(EventLoopGroup eventLoopGroup, Class<? extends Channel> datagramChannelClass) {
        // Create and return a new RpcEventLoopManager with UDP-specific configuration
        // Passing null for tcpChannelClass, serverChannelClass, and childHandler as they are not needed for UDP
        return of(eventLoopGroup, null, null, null, datagramChannelClass);
    }

    /**
     * Returns the appropriate server channel class based on the type of event loop group.
     * This method determines the type of channel to use for the server socket based on the
     * implementation of the event loop group provided. It supports NIO, Epoll, and KQueue implementations.
     *
     * @return The Class object representing the appropriate server channel implementation
     * @throws RpcException if the event loop group type is not supported
     */
    public Class<? extends ServerChannel> getServerChannelClass() {
        // If the server channel class has already been determined and set, return it
        if (serverChannelClass != null) {
            return serverChannelClass;
        }
        // Check if the event loop group is NIO-based
        if (this.eventLoopGroup instanceof NioEventLoopGroup) {
            return NioServerSocketChannel.class;
        }
        // Check if the event loop group is Epoll-based (Linux-specific)
        if (this.eventLoopGroup instanceof EpollEventLoopGroup) {
            return EpollServerSocketChannel.class;
        }
        // Check if the event loop group is KQueue-based (macOS/BSD-specific)
        if (this.eventLoopGroup instanceof KQueueEventLoopGroup) {
            return KQueueServerSocketChannel.class;
        }
        // If none of the supported event loop group types match, throw an exception
        throw new RpcException(RpcErrorEnum.CONNECT, "group types are not supported at the moment");
    }


    /**
     * Returns the appropriate channel class based on the type of eventLoopGroup.
     * This method determines which type of socket channel to use based on the
     * implementation of the eventLoopGroup provided.
     *
     * @return The appropriate Channel class that matches the eventLoopGroup type
     * @throws RpcException if the eventLoopGroup type is not supported
     */
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


    /**
     * Returns the appropriate DatagramChannel class based on the type of EventLoopGroup.
     * This method checks the type of EventLoopGroup and returns the corresponding DatagramChannel implementation.
     *
     * @return The Class object of the appropriate DatagramChannel implementation
     * @throws RpcException if the EventLoopGroup type is not supported for UDP operations
     */
    public Class<? extends Channel> getDatagramChannelClass() {
        // If the datagramChannelClass is already set, return it directly
        if (this.datagramChannelClass != null) {
            return datagramChannelClass;
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
        // Check if the worker event loop group is not null and not already in shutdown process
        if (workerEventLoopGroup != null && (!workerEventLoopGroup.isShutdown() && !workerEventLoopGroup.isShuttingDown())) {
            // Shutdown the worker event loop group gracefully
                workerEventLoopGroup.shutdownGracefully();

        }
    }
}
