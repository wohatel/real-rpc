package com.github.wohatel.tcp.builder;

import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcVivoHandler;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * description
 *
 * @author yaochuang 2025/12/27 17:13
 */
@Builder
@Getter
public class RpcServerConnectConfig {
    private String host;
    private int port;
    /**
     * is used to handle the heart beat or alive channel
     */
    private RpcVivoHandler vivoHandler;
    private List<ChannelOptionAndValue<Object>> channelOptions;
    private List<ChannelOptionAndValue<Object>> childChannelOptions;
}
