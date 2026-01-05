package com.github.wohatel.tcp.builder;

import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcVivoHandler;
import lombok.Builder;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.List;

/**
 * description
 *
 * @author yaochuang 2025/12/27 17:13
 */
@Builder
@Getter
public class RpcClientConnectConfig {
    private String host;
    private int port;
    /**
     * is used to handle the heart beat or alive channel
     */
    private RpcVivoHandler rpcVivoHandler;
    private List<ChannelOptionAndValue<Object>> channelOptions;
    /**
     * localAddress is used to bind the socket to a local address connect to remote address; default null
     */
    protected SocketAddress localAddress;
}
