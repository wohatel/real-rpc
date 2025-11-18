package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;

import java.util.Objects;


/**
 * A manager class for RPC event loops that handles different types of event loop groups and their associated channel classes.
 * This class provides factory methods to create instances for server, client, and UDP configurations.
 */
@Data
public class RpcMutiEventLoopManager {
    // Event loop group for accepting connections
    private EventLoopGroup eventLoopGroup;
    // Worker event loop group for handling incoming connections
    private EventLoopGroup workerEventLoopGroup;
    // Class for server channel implementation
    private Class<? extends ServerChannel> channelClass;

    /**
     * Creates and returns an instance of RpcMutiEventLoopManager with the specified parameters.
     *
     * @param eventLoopGroup      The main event loop group for handling I/O operations
     * @param childEventLoopGroup The child event loop group for handling child channel operations
     * @param channelClass        The class of the server channel to be used
     * @throws NullPointerException if any of the parameters are null
     */
    public RpcMutiEventLoopManager(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup, Class<? extends ServerChannel> channelClass) {
        // Validate that required parameters are not null
        Objects.requireNonNull(eventLoopGroup);
        Objects.requireNonNull(childEventLoopGroup);
        // Set the provided parameters to the instance
        this.eventLoopGroup = eventLoopGroup;
        this.workerEventLoopGroup = childEventLoopGroup;
        this.channelClass = channelClass;
    }

    /**
     * Creates a server-side RpcEventLoopManager with the specified event loops and channel class.
     *
     * @param eventLoopGroup      The main event loop group for handling server connections
     * @param childEventLoopGroup The child event loop group for handling incoming connections
     */
    public RpcMutiEventLoopManager(EventLoopGroup eventLoopGroup, EventLoopGroup childEventLoopGroup) {
        this(eventLoopGroup, childEventLoopGroup, null);
    }

    /**
     * Static factory method to create an RpcEventLoopManager with default values
     *
     * @param eventLoopGroup The EventLoopGroup to be used by the RpcEventLoopManager
     *                       and default values for other parameters
     */
    public RpcMutiEventLoopManager(EventLoopGroup eventLoopGroup) {
        // Delegate to the main factory method with null values for unspecified parameters
        this(eventLoopGroup, eventLoopGroup);
    }

    /**
     * Creates a default RpcEventLoopManager instance using NioEventLoopGroup.
     * This is a factory method that provides a convenient way to create a manager with default NIO implementation.
     *
     */
    public RpcMutiEventLoopManager() {
        this(new NioEventLoopGroup());
    }

    /**
     * Returns the appropriate server channel class based on the type of event loop group.
     * This method determines the type of channel to use for the server socket based on the
     * implementation of the event loop group provided. It supports NIO, Epoll, and KQueue implementations.
     *
     * @return The Class object representing the appropriate server channel implementation
     * @throws RpcException if the event loop group type is not supported
     */
    public Class<? extends ServerChannel> getChannelClass() {
        // If the server channel class has already been determined and set, return it
        if (channelClass != null) {
            return channelClass;
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
