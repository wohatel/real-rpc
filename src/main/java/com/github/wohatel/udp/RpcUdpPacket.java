package com.github.wohatel.udp;

import lombok.Data;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

/** * upd request msg wrapper
 *
 * @author yaochuang 2025/09/17 15:25
 */
@Data
@Accessors(chain = true)
public class RpcUdpPacket<T> {
    private T msg;
    private InetSocketAddress sender;
}
