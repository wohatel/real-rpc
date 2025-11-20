package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcMutiEventLoopManager;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.List;

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
    private final List<ChannelOptionAndValue<Object>> channelOptions;
    private final List<ChannelOptionAndValue<Object>> childChannelOptions;
    private final RpcMutiEventLoopManager eventLoopManager;

    public RpcServer(int port) {
        this(port, RpcMutiEventLoopManager.of());
    }

    public RpcServer(int port, RpcMutiEventLoopManager eventLoopManager) {
        this(null, port, eventLoopManager, null, null);
    }

    public RpcServer(String host, int port, RpcMutiEventLoopManager eventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, List<ChannelOptionAndValue<Object>> childChannelOptions) {
        super(host, port);
        this.eventLoopManager = eventLoopManager;
        this.channelOptions = channelOptions;
        this.childChannelOptions = childChannelOptions;
    }

    @SneakyThrows
    @SuppressWarnings("all")
    public ChannelFuture start() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "rpcServer: do not repeat the start");
        }
        InetSocketAddress address = StringUtils.isBlank(host) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
        ServerBootstrap b = new ServerBootstrap();
        b.group(eventLoopManager.getEventLoopGroup(), eventLoopManager.getWorkerEventLoopGroup()).channel(eventLoopManager.getChannelClass());
        b.childOption(ChannelOption.TCP_NODELAY, true);
        b.localAddress(address).childHandler(rpcMsgChannelInitializer);
        if (!EmptyVerifyUtil.isEmpty(channelOptions)) {
            for (ChannelOptionAndValue channelOption : channelOptions) {
                b.option(channelOption.getChannelOption(), channelOption.getValue());
            }
        }
        if (!EmptyVerifyUtil.isEmpty(childChannelOptions)) {
            for (ChannelOptionAndValue childOption : childChannelOptions) {
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
