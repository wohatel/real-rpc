package com.github.wohatel.interaction.common;

import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.InetSocketAddress;

/**
 * RpcUdpWaiter is a generic class responsible for handling UDP message sending operations.
 * It utilizes the Lombok @Data annotation to generate getters, setters, toString(), equals(), and hashCode() methods.
 *
 * @param <T> The type of message to be sent through UDP
 */
@Data
@AllArgsConstructor
public class RpcUdpWaiter<T> {

    /**
     * The ChannelHandlerContext which provides the context information for the channel.
     * This is a final field, meaning it can only be set once and cannot be modified afterward.
     */
    private final ChannelHandlerContext channelHandlerContext;

    /**
     * Sends a message of type T to the specified InetSocketAddress via UDP.
     *
     * @param t  The message object to be sent
     * @param to The InetSocketAddress representing the destination of the message
     */
    public void sendMsg(T t, InetSocketAddress to) {
        RpcMsgTransManager.sendUdpMsg(channelHandlerContext.channel(), t, to);
    }

}
