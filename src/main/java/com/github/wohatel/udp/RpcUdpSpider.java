package com.github.wohatel.udp;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;

/**
 * @author yaochuang
 */
public class RpcUdpSpider {
    private final MultiThreadIoEventLoopGroup eventLoopGroup;
    private final SimpleChannelInboundHandler<DatagramPacket> clientChannelHandler;
    private Channel channel;
    private final Class<? extends Channel> channelClass;

    public RpcUdpSpider(MultiThreadIoEventLoopGroup eventLoopGroup, SimpleChannelInboundHandler<DatagramPacket> clientChannelHandler) {
        this.eventLoopGroup = eventLoopGroup;
        this.clientChannelHandler = clientChannelHandler;
        this.channelClass = getChannelClass();
    }

    @SneakyThrows
    public ChannelFuture bind(int port) {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "重复绑定");
        }
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup);
        b.channel(channelClass).option(ChannelOption.SO_BROADCAST, true);
        b.handler(this.clientChannelHandler);
        ChannelFuture bind = b.bind(port);
        this.channel = bind.channel();
        return bind;
    }

    @SneakyThrows
    public ChannelFuture bindAsClient() {
        // 表示随机绑定一个端口,一般是知道了服务端地址,然后客户端随机分配一个端口通信
        return bind(0);
    }

    /**
     * 发送消息
     */
    public void sendMsg(String msg, InetSocketAddress to) {
        RpcMsgTransUtil.sendUdpMsg(this.channel, msg, to);
    }


    /**
     * 返回类型
     */
    protected Class<? extends Channel> getChannelClass() {
        if (this.eventLoopGroup.isIoType(NioIoHandler.class)) {
            return NioDatagramChannel.class;
        }
        if (this.eventLoopGroup.isIoType(EpollIoHandler.class)) {
            return EpollDatagramChannel.class;
        }
        if (this.eventLoopGroup.isIoType(KQueueIoHandler.class)) {
            return KQueueDatagramChannel.class;
        }
        throw new RpcException(RpcErrorEnum.RUNTIME, "udp的eventLoopGroup类型不支持");
    }
}
