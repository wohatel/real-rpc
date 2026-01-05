package com.github.wohatel.tcp;

import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.util.RandomUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;
import lombok.Getter;

import java.util.Objects;


/**
 * A class responsible for receiving RPC data and managing network communication channels.
 * It handles different types of RPC messages including file transfers, simple requests, and session requests.
 */
@Data
public class RpcDataReceiver {

    protected final String uniqueId;  // Unique identifier for this receiver instance

    @Getter
    protected Channel channel;       // Network channel for communication

    // Channel initializer for setting up RPC message handlers
    protected RpcMsgChannelInitializer rpcMsgChannelInitializer;

    /**
     * Constructor for creating a receiver with specified host and port
     *
     */
    protected RpcDataReceiver(RpcMsgChannelInitializer rpcMsgChannelInitializer) {
        this.uniqueId = RandomUtil.randomUUIDWithTime();
        this.rpcMsgChannelInitializer = Objects.requireNonNullElseGet(rpcMsgChannelInitializer, RpcMsgChannelInitializer::new);
    }

    /**
     * Sets up a handler for receiving file transfer requests
     *
     * @param rpcFileRequestMsgHandler The handler for file transfer requests
     */
    public void onFileReceive(RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        rpcMsgChannelInitializer.onFileReceive(rpcFileRequestMsgHandler);
    }

    /**
     * Sets up a handler for receiving simple RPC requests
     *
     * @param rpcSimpleRequestMsgHandler The handler for simple requests
     */
    public void onRequestReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMsgChannelInitializer.onRequestReceive(rpcSimpleRequestMsgHandler);
    }

    /**
     * Sets up a handler for receiving session-related requests
     *
     * @param rpcSessionRequestMsgHandler The handler for session requests
     */
    public void onSessionRequestReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMsgChannelInitializer.onSessionRequestReceive(rpcSessionRequestMsgHandler);
    }

    /**
     * Closes the communication channel if it exists
     *
     * @return ChannelFuture representing the asynchronous close operation, or null if channel doesn't exist
     */
    public ChannelFuture close() {
        if (channel != null) {
            return channel.close();
        }
        return null;
    }
}
