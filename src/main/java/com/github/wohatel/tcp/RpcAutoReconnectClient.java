package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcVivoHandler;
import com.github.wohatel.interaction.common.RpcSocketEventLoopManager;
import com.github.wohatel.tcp.strategy.FixedDelayReconnectStrategy;
import com.github.wohatel.tcp.strategy.ReconnectStrategy;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A client implementation that automatically reconnects when the connection is lost.
 * Extends the basic RPC client functionality with auto-reconnect capabilities.
 */
@Slf4j
public class RpcAutoReconnectClient extends RpcDefaultClient {

    /**
     * Netty Bootstrap instance used for connection initialization
     * This is lazily initialized when first needed
     */
    private Bootstrap bootstrap;

    @Getter
    private boolean closed;

    @Getter
    @Setter
    private ReconnectStrategy reconnectStrategy;

    /**
     * Constructor with host and port parameters
     *
     * @param host The target host to connect to
     * @param port The target port to connect to
     */
    public RpcAutoReconnectClient(String host, int port) {
        this(host, port, null);
    }

    /**
     * Constructor with host, port, and event loop manager parameters
     *
     * @param host The target host to connect to
     * @param port The target port to connect to
     */
    public RpcAutoReconnectClient(String host, int port, RpcVivoHandler rpcVivoHandler) {
        this(host, port, rpcVivoHandler, RpcSocketEventLoopManager.of());
    }

    /**
     * Constructor with host, port, and event loop manager parameters
     *
     * @param host             The target host to connect to
     * @param port             The target port to connect to
     * @param eventLoopManager The event loop manager for handling I/O operations
     */
    public RpcAutoReconnectClient(String host, int port, RpcVivoHandler rpcVivoHandler, RpcSocketEventLoopManager eventLoopManager) {
        this(host, port, rpcVivoHandler, eventLoopManager, null);
    }

    /**
     * Constructor with host, port, and event loop manager parameters
     *
     * @param host             The target host to connect to
     * @param port             The target port to connect to
     * @param eventLoopManager The event loop manager for handling I/O operations
     */
    public RpcAutoReconnectClient(String host, int port, RpcVivoHandler rpcVivoHandler, RpcSocketEventLoopManager eventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions) {
        super(host, port, rpcVivoHandler, eventLoopManager, channelOptions);
    }

    /**
     * Attempts to establish a connection to the remote server.
     * This method handles the connection setup process, including creating a Bootstrap if necessary,
     * configuring it with various options, and initiating the connection.
     *
     * @return ChannelFuture representing the pending connection operation
     * @throws RpcException if there is already an active connection
     */
    private ChannelFuture tryConnect() {
        // Check if there is already an active connection
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "the connection is alive: RpcAutoReconnectClient");
        }
        // Initialize Bootstrap if it doesn't exist
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            // Configure the event loop group
            bootstrap.group(eventLoopManager.getEventLoopGroup());
            // Set the channel class
            bootstrap.channel(eventLoopManager.getChannelClass());
            // Disable Nagle's algorithm for lower latency
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            // Apply additional channel options if any
            if (!EmptyVerifyUtil.isEmpty(channelOptions)) {
                // Iterate through all channel options and apply them
                for (ChannelOptionAndValue<Object> channelOption : channelOptions) {
                    bootstrap.option(channelOption.getChannelOption(), channelOption.getValue());
                }
            }
        }
        // Set the channel handler
        bootstrap.handler(this.rpcMsgChannelInitializer);
        // Create the remote address
        InetSocketAddress remote = InetSocketAddress.createUnresolved(host, port);
        // Initiate the connection, using local address if specified
        return localAddress == null ? bootstrap.connect(remote) : bootstrap.connect(remote, localAddress);
    }


    @Override
    public ChannelFuture connect() {
        throw new RpcException(RpcErrorEnum.CONNECT, "rpcAutoReconnectClient connect link is not supported, please use it instead autoReconnect()");
    }

    /**
     * Automatic reconnection when broken
     * This method handles the automatic reconnection logic when the connection is lost
     */
    public void autoReconnect() {
        if (closed) {
            // If the connection was manually closed, do not attempt to reconnect
            return;
        }
        if (reconnectStrategy == null) {
            // If no reconnect strategy is set, initialize with a default strategy
            // Default strategy: retry every 5 seconds
            reconnectStrategy = new FixedDelayReconnectStrategy(5000);
        }
        if (!reconnectStrategy.shouldReconnect()) {
            // Check if the current strategy allows reconnection
            // If not, exit the method
            return;
        }
        // Attempt to establish a new connection
        ChannelFuture future = tryConnect();
        // Add a listener to handle the connection attempt result
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                // If connection is successful
                Channel newChannel = future.channel();
                // Update the current channel reference
                this.channel = newChannel;
                // Notify the strategy about successful connection
                reconnectStrategy.onSuccess();
                // Schedule a new reconnection attempt when this channel closes
                newChannel.closeFuture().addListener(cf -> scheduleReconnect());
            } else {
                // If connection fails
                // Notify the strategy about the failure
                reconnectStrategy.onFailure();
                // Schedule a new reconnection attempt
                scheduleReconnect();
            }
        });
    }

    @Override
    public ChannelFuture close() {
        this.closed = true;
        return super.close();
    }

    /**
     * Schedules a reconnection attempt using the configured reconnection strategy.
     * This method calculates the appropriate delay for reconnection based on the strategy
     * and schedules the reconnection task to be executed on the next available event loop.
     */
    private void scheduleReconnect() {
        // Calculate the delay for reconnection using the reconnection strategy
        long delay = reconnectStrategy.nextDelayMillis();
        // Get the next event loop from the event loop group
        EventLoop loop = bootstrap.config().group().next();
        // Schedule the autoReconnect method to be called after the calculated delay
        loop.schedule(this::autoReconnect, delay, TimeUnit.MILLISECONDS);
    }
}
