package com.github.wohatel.udp;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.util.ByteBufDecoder;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Data;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author yaochuang
 */
@Data
public class RpcUdpSpider<T> {
    protected final MultiThreadIoEventLoopGroup eventLoopGroup;
    protected final ChannelInitializer<DatagramChannel> channelInitializer;
    protected Channel channel;
    protected final Class<? extends Channel> channelClass;
    protected final List<ChannelOptionAndValue<Object>> channelOptions;

    public RpcUdpSpider(MultiThreadIoEventLoopGroup eventLoopGroup, ChannelInitializer<DatagramChannel> channelInitializer) {
        this(eventLoopGroup, channelInitializer, null);
    }

    public RpcUdpSpider(MultiThreadIoEventLoopGroup eventLoopGroup, ChannelInitializer<DatagramChannel> channelInitializer, List<ChannelOptionAndValue<Object>> channelOptions) {
        this.eventLoopGroup = eventLoopGroup;
        this.channelInitializer = channelInitializer;
        this.channelClass = getChannelClass();
        this.channelOptions = channelOptions;
    }

    /**
     * 构建一个简单的udp
     */
    public static RpcUdpSpider<String> buildStringSpider(SimpleChannelInboundHandler<RpcUdpPacket<String>> simpleChannelInboundHandler) {
        return buildSpider(new TypeReference<String>() {
        }, simpleChannelInboundHandler);
    }

    /**
     * 构建一个简单的udp
     */
    public static RpcUdpSpider<RpcRequest> buildRpcRequestSpider(SimpleChannelInboundHandler<RpcUdpPacket<RpcRequest>> simpleChannelInboundHandler) {
        return buildSpider(new TypeReference<RpcRequest>() {
        }, simpleChannelInboundHandler);
    }

    /**
     * 构建一个简单的udp
     */
    public static <T> RpcUdpSpider<T> buildSpider(TypeReference<T> clazz, SimpleChannelInboundHandler<RpcUdpPacket<T>> simpleChannelInboundHandler) {
        return new RpcUdpSpider<>(new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory()), new ChannelInitializer<>() {
            @Override
            protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                SimpleChannelInboundHandler<DatagramPacket> decoder = new SimpleChannelInboundHandler<>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                        T decode = ByteBufDecoder.decode(datagramPacket.content(), clazz);
                        RpcUdpPacket<T> rpcUdpPacket = new RpcUdpPacket<>();
                        rpcUdpPacket.setMsg(decode);
                        rpcUdpPacket.setSender(datagramPacket.sender());
                        channelHandlerContext.fireChannelRead(rpcUdpPacket);
                    }
                };
                datagramChannel.pipeline().addLast(decoder).addLast(simpleChannelInboundHandler);
            }
        });
    }

    @SneakyThrows
    @SuppressWarnings("all")
    public ChannelFuture bind(int port) {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "重复绑定");
        }
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup);
        b.channel(channelClass);
        // 默认开启广播
        b.option(ChannelOption.SO_BROADCAST, true);
        if (!EmptyVerifyUtil.isEmpty(channelOptions)) {
            for (ChannelOptionAndValue channelOption : channelOptions) {
                b.option(channelOption.getChannelOption(), channelOption.getValue());
            }
        }
        b.handler(channelInitializer);
        ChannelFuture bind = b.bind(port);
        this.channel = bind.channel();
        return bind;
    }

    @SneakyThrows
    public ChannelFuture bind() {
        // 表示随机绑定一个端口
        return bind(0);
    }

    /**
     * 发送消息
     */
    public void sendMsg(T msg, InetSocketAddress to) {
        sendGeneralMsg(this.channel, msg, to);
    }

    /**
     *
     * @param channel 通道
     * @param msg     消息提
     * @param to      目标
     * @param <T>     泛型
     */
    public static <T> void sendGeneralMsg(Channel channel, T msg, InetSocketAddress to) {
        RpcMsgTransUtil.sendUdpMsg(channel, msg, to);
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
