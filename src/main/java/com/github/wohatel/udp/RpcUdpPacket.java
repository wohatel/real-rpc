package com.github.wohatel.udp;

import lombok.Data;

import java.net.InetSocketAddress;

/** * upd request msg wrapper
 *
 * @author yaochuang 2025/09/17 15:25
 */
@Data
public class RpcUdpPacket<T> {
    private T msg;
    private InetSocketAddress sender;
}
