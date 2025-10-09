package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcAutoReconnectClient extends RpcDefaultClient {

    @Getter
    @Setter
    private Long autoReconnectInterval = 5000L;

    /**     * Whether automatic reconnection is allowed
     */
    @Getter
    @Setter
    private boolean allowAutoConnect = true;

    private Bootstrap bootstrap;

    public RpcAutoReconnectClient(String host, int port, MultithreadEventLoopGroup eventLoopGroup) {
        this(host, port, eventLoopGroup, null);
    }

    public RpcAutoReconnectClient(String host, int port, MultithreadEventLoopGroup eventLoopGroup, List<ChannelOptionAndValue<Object>> channelOptions) {
        super(host, port, eventLoopGroup, channelOptions);
    }

    /**     * Try the link
     */
    private ChannelFuture tryConnect() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "the connection is alive: RpcAutoReconnectClient");
        }
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup);
            bootstrap.channel(channelClass);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            if (!EmptyVerifyUtil.isEmpty(channelOptions)) {
                for (ChannelOptionAndValue<Object> channelOption : channelOptions) {
                    bootstrap.option(channelOption.getChannelOption(), channelOption.getValue());
                }
            }
        }
        bootstrap.handler(this.rpcMsgChannelInitializer);
        InetSocketAddress remote = InetSocketAddress.createUnresolved(host, port);
        return localAddress == null ? bootstrap.connect(remote) : bootstrap.connect(remote, localAddress);
    }


    @Override
    public ChannelFuture connect() {
        throw new RpcException(RpcErrorEnum.CONNECT, "rpcAutoReconnectClient connect link is not supported, please use it instead autoReconnect()");
    }

    /**     * Automatic reconnection when broken
     */
    @SneakyThrows
    public void autoReconnect() {
        if (!allowAutoConnect) {
            // 如果被设置为不允许重连,则直接返回
            return;
        }
        ChannelFuture future = tryConnect();
        future.addListener((ChannelFutureListener) connectFuture -> {
            Channel newChannel = connectFuture.channel();
            this.channel = newChannel;
            if (connectFuture.isSuccess()) {
                log.info("the connection was successful: " + host + ":" + port);
                // 监听关闭，关闭后自动重连（异步调度，避免递归）
                newChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.error("the connection is broken and will try to reconnect...");
                    closeFuture.channel().eventLoop().execute(this::autoReconnect);
                });
            } else {
                Throwable cause = connectFuture.cause();
                log.error("connection failed: " + host + ":" + port, cause);

                // 清理失败 channel
                if (newChannel != null && newChannel.isOpen()) {
                    newChannel.close();
                }
                // 使用 Bootstrap 绑定的 EventLoopGroup 安排重连任务（安全）
                EventLoop eventLoop = bootstrap.config().group().next();
                eventLoop.schedule(() -> {
                    log.info("await " + autoReconnectInterval + "ms reconnect...");
                    autoReconnect();
                }, autoReconnectInterval, TimeUnit.MILLISECONDS);
            }
        });
    }

    /**     * Take the initiative to shut down
     */
    @Override
    public ChannelFuture close() {
        this.setAllowAutoConnect(false);
        return super.close();
    }
}
