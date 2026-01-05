package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcMutiEventLoopManager;
import com.github.wohatel.tcp.builder.RpcServerConnectConfig;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;

/**
 * Bind the server to the listening port and configure the channel
 * to notify the EchoServerHandler instance of inbound messages
 *
 * @author yaochuang
 * @version 1.0.0
 * @since 2021/5/25上午12:55
 */
@Getter
@Slf4j
public class RpcServer extends RpcDataReceiver {
    private final RpcServerConnectConfig connectConfig;
    private final RpcMutiEventLoopManager eventLoopManager;

    public RpcServer(RpcServerConnectConfig connectConfig, RpcMutiEventLoopManager eventLoopManager) {
        super(new RpcMsgChannelInitializer(connectConfig.getVivoHandler()));
        this.eventLoopManager = eventLoopManager;
        this.connectConfig = connectConfig;
    }

    @SneakyThrows
    @SuppressWarnings("all")
    public ChannelFuture start() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "rpcServer: do not repeat the start");
        }
        InetSocketAddress address = StringUtils.isBlank(connectConfig.getHost()) ? new InetSocketAddress(connectConfig.getPort()) : new InetSocketAddress(connectConfig.getHost(), connectConfig.getPort());
        ServerBootstrap b = new ServerBootstrap();
        b.group(eventLoopManager.getEventLoopGroup(), eventLoopManager.getWorkerEventLoopGroup()).channel(eventLoopManager.getChannelClass());
        b.childOption(ChannelOption.TCP_NODELAY, true);
        b.localAddress(address).childHandler(rpcMsgChannelInitializer);
        if (!EmptyVerifyUtil.isEmpty(connectConfig.getChannelOptions())) {
            for (ChannelOptionAndValue channelOption : connectConfig.getChannelOptions()) {
                b.option(channelOption.getChannelOption(), channelOption.getValue());
            }
        }
        if (!EmptyVerifyUtil.isEmpty(connectConfig.getChildChannelOptions())) {
            for (ChannelOptionAndValue childOption : connectConfig.getChildChannelOptions()) {
                b.childOption(childOption.getChannelOption(), childOption.getValue());
            }
        }
        ChannelFuture future = b.bind().addListener(futureListener -> {
            if (!futureListener.isSuccess()) {
                Throwable cause = futureListener.cause();
                log.error("netty service start failure, cause：", cause);
            }
        });
        this.channel = future.channel();
        return future;
    }
}
