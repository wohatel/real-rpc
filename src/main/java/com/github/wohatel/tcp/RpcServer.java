package com.github.wohatel.tcp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 绑定服务器到监听的端口，配置Channel，将入站消息通知给EchoServerHandler实例
 *
 * @author yaochuang
 * @version 1.0.0
 * @since 2021/5/25上午12:55
 */
@Getter
@Slf4j
public class RpcServer extends RpcDataReceiver {
    private final MultithreadEventLoopGroup group;
    private final MultithreadEventLoopGroup childGroup;
    private final Class<? extends ServerChannel> serverChannelClass;
    private final List<ChannelOptionAndValue<Object>> channelOptions;
    private final List<ChannelOptionAndValue<Object>> childChannelOptions;

    public RpcServer(int port, MultithreadEventLoopGroup group, MultithreadEventLoopGroup childGroup) {
        this(null, port, group, childGroup, null, null);
    }

    public RpcServer(String host, int port, MultithreadEventLoopGroup group, MultithreadEventLoopGroup childGroup, List<ChannelOptionAndValue<Object>> channelOptions, List<ChannelOptionAndValue<Object>> childChannelOptions) {
        super(host, port);
        this.group = group;
        this.childGroup = childGroup;
        serverChannelClass = getServerChannelClass();
        this.channelOptions = channelOptions;
        this.childChannelOptions = childChannelOptions;
    }

    /**
     * 开启nettyServer
     */
    @SneakyThrows
    @SuppressWarnings("all")
    public ChannelFuture start() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "rpcServer: do not repeat the start");
        }
        InetSocketAddress address = StringUtils.isBlank(host) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
        ServerBootstrap b = new ServerBootstrap();
        b.group(group, childGroup).channel(serverChannelClass);
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
        this.channel = future.channel(); // 用于关闭server
        return future;
    }

    /**
     * 返回类型
     */
    protected Class<? extends ServerChannel> getServerChannelClass() {
        if (this.group instanceof NioEventLoopGroup) {
            return NioServerSocketChannel.class;
        }
        if (this.group instanceof EpollEventLoopGroup) {
            return EpollServerSocketChannel.class;
        }
        if (this.group instanceof KQueueEventLoopGroup) {
            return KQueueServerSocketChannel.class;
        }
        throw new RpcException(RpcErrorEnum.CONNECT, "group types are not supported at the moment");
    }
}
