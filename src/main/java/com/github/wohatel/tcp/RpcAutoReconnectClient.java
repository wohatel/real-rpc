package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
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

    @Getter
    @Setter
    private Long autoReconnectInterval = 5000L;

    /**     
     * Whether automatic reconnection is allowed
     * This flag can be used to disable auto-reconnect functionality when needed
     */
    @Getter
    @Setter
    private boolean allowAutoConnect = true;

    /**
     * Netty Bootstrap instance used for connection initialization
     * This is lazily initialized when first needed
     */
    private Bootstrap bootstrap;

    /**
     * Constructor with host and port parameters
     *
     * @param host The target host to connect to
     * @param port The target port to connect to
     */
    public RpcAutoReconnectClient(String host, int port) {
        this(host, port, new RpcEventLoopManager());
    }

    /**
     * Constructor with host, port, and event loop manager parameters
     *
     * @param host                The target host to connect to
     * @param port                The target port to connect to
     * @param rpcEventLoopManager The event loop manager for handling I/O operations
     */
    public RpcAutoReconnectClient(String host, int port, RpcEventLoopManager rpcEventLoopManager) {
        this(host, port, rpcEventLoopManager, null);
    }

    /**
     * Constructor with all parameters including channel options
     *
     * @param host                The target host to connect to
     * @param port                The target port to connect to
     * @param rpcEventLoopManager The event loop manager for handling I/O operations
     * @param channelOptions      List of channel options and their values to configure the connection
     */
    public RpcAutoReconnectClient(String host, int port, RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions) {
        super(host, port, rpcEventLoopManager, channelOptions);
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
            bootstrap.group(rpcEventLoopManager.getEventLoopGroup());
            // Set the channel class
            bootstrap.channel(rpcEventLoopManager.getChannelClass());
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
     */
    @SneakyThrows
    public void autoReconnect() {
        if (!allowAutoConnect) {
            // 如果被设置为不允许重连,则直接返回
            return;
        }
        ChannelFuture future = tryConnect();
        future.addListener((ChannelFutureListener) connectFuture -> {
            Channel newChannel = connectFuture.channel();
            this.channel = newChannel;
            if (connectFuture.isSuccess()) {
                log.info("the connection was successful--{}:{} ", host, port);
                // 监听关闭，关闭后自动重连（异步调度，避免递归）
                newChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.error("the connection is broken and will try to reconnect...");
                    closeFuture.channel().eventLoop().execute(this::autoReconnect);
                });
            } else {
                Throwable cause = connectFuture.cause();
                log.error("connection failed: {}:{}", host, port, cause);

                // 清理失败 channel
                if (newChannel != null && newChannel.isOpen()) {
                    newChannel.close();
                }
                // 使用 Bootstrap 绑定的 EventLoopGroup 安排重连任务（安全）
                EventLoop eventLoop = bootstrap.config().group().next();
                eventLoop.schedule(() -> {
                    log.info("await " + autoReconnectInterval + "ms reconnect...");
                    autoReconnect();
                }, autoReconnectInterval, TimeUnit.MILLISECONDS);
            }
        });
    }


    /**
     * Overrides the close method to disable auto-connection before closing the channel.
     * This ensures that after the channel is closed, it won't automatically try to reconnect.
     *
     * @return ChannelFuture representing the asynchronous close operation
     */
    @Override
    public ChannelFuture close() {
        // Disable auto-connection before closing the channel
        this.setAllowAutoConnect(false);
        // Call the parent class's close method to perform the actual close operation
        return super.close();
    }
}
