package com.murong.rpc.client;

import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Log
public class RpcAutoReconnectClient extends RpcDefaultClient {

    @Getter
    @Setter
    private Long autoReconnectInterval = 5000L;

    /**
     * 是否允许自动重连
     */
    @Getter
    @Setter
    private boolean allowAutoConnect = true;

    private Bootstrap bootstrap;

    public RpcAutoReconnectClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        super(host, port, nioEventLoopGroup);
    }

    public RpcAutoReconnectClient(String host, int port) {
        super(host, port);
    }

    /**
     * 尝试链接
     */
    private ChannelFuture tryConnect() {
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            bootstrap.group(nioEventLoopGroup);
            bootstrap.channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
            bootstrap.handler(this.rpcMsgChannelInitializer);
        }
        return bootstrap.connect(host, port);
    }


    @Override
    public ChannelFuture connect() {
        throw new RuntimeException("RpcAutoReconnectClient不支持connect链接,请改用autoReconnect()");
    }

    /**
     * 断线自动重连
     */
    public void autoReconnect() {
        if (!allowAutoConnect) {
            // 如果被设置为不允许重连,则直接返回
            return;
        }
        ChannelFuture future = tryConnect();
        future.addListener((ChannelFutureListener) connectFuture -> {
            Channel newChannel = connectFuture.channel();
            if (connectFuture.isSuccess()) {
                log.info("连接成功: " + host + ":" + port);
                initClient(newChannel);
                // 监听关闭，关闭后自动重连（异步调度，避免递归）
                newChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.warning("连接断开，将尝试重连...");
                    closeFuture.channel().eventLoop().execute(this::autoReconnect);
                });
            } else {
                Throwable cause = connectFuture.cause();
                log.log(Level.WARNING, "连接失败: " + host + ":" + port, cause);

                // 清理失败 channel
                if (newChannel != null && newChannel.isOpen()) {
                    newChannel.close();
                }
                // 使用 Bootstrap 绑定的 EventLoopGroup 安排重连任务（安全）
                EventLoop eventLoop = bootstrap.config().group().next();
                eventLoop.schedule(() -> {
                    log.info("等待 " + autoReconnectInterval + "ms 后重连...");
                    autoReconnect();
                }, autoReconnectInterval, TimeUnit.MILLISECONDS);
            }
        });
    }

    /**
     * 主动关停
     */
    @Override
    public void close() {
        this.setAllowAutoConnect(false);
        super.close();
    }


}
