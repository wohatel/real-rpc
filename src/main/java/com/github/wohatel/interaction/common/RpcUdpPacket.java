package com.github.wohatel.interaction.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;


/**
 * A generic UDP packet class for RPC (Remote Procedure Call) communication.
 * This class uses Lombok annotations for reducing boilerplate code.
 *
 * @param <T> the type of the message payload in the packet
 */
@Data
@Accessors(chain = true)
public class RpcUdpPacket<T> {
    // The message payload of the packet
    private T msg;
    // The network address of the sender
    private InetSocketAddress sender;
}
