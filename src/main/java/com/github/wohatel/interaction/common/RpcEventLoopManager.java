package com.github.wohatel.interaction.common;

import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * A manager class for RPC event loops that handles different types of event loop groups and their associated channel classes.
 * This class provides factory methods to create instances for server, client, and UDP configurations.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RpcEventLoopManager<T extends Channel> {
    // Event loop group for accepting connections
    @Getter
    protected EventLoopGroup eventLoopGroup;
    // Class for client channel implementation
    protected Class<T> channelClass;

    /**
     * Gracefully shuts down the event loop groups if they are not already shutting down or shut down.
     * This method ensures a clean termination of all active I/O operations and releases resources.
     */
    public void shutdownGracefully() {
        // Check if the main event loop group is not null and not already in shutdown process
        if (eventLoopGroup != null && (!eventLoopGroup.isShutdown() && !eventLoopGroup.isShuttingDown())) {
            // Shutdown the main event loop group gracefully
            RunnerUtil.execSilentVoidException(eventLoopGroup::shutdownGracefully, e -> log.error("close eventLoopGroup error:", e));
        }
    }
}
