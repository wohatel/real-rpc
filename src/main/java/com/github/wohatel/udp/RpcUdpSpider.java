package com.github.wohatel.udp;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.util.ByteBufUtil;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.util.List;

/** * a udp server for turn on with an port and send msg
 *
 * @author yaochuang
 */
@Data
public class RpcUdpSpider<T> {
    protected final RpcEventLoopManager rpcEventLoopManager;
    protected final ChannelInitializer<DatagramChannel> channelInitializer;
    protected Channel channel;
    protected final List<ChannelOptionAndValue<Object>> channelOptions;

    public RpcUdpSpider(RpcEventLoopManager rpcEventLoopManager, ChannelInitializer<DatagramChannel> channelInitializer) {
        this(rpcEventLoopManager, null, channelInitializer);
    }

    public RpcUdpSpider(RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, ChannelInitializer<DatagramChannel> channelInitializer) {
        this.rpcEventLoopManager = rpcEventLoopManager;
        this.channelInitializer = channelInitializer;
        this.channelOptions = channelOptions;
    }

    public static <T> RpcUdpSpider<T> buildSpider(TypeReference<T> clazz, SimpleChannelInboundHandler<RpcUdpPacket<T>> simpleChannelInboundHandler) {
        return buildSpider(clazz, RpcEventLoopManager.of(new NioEventLoopGroup()), null, simpleChannelInboundHandler);
    }

    /**     * Build a simple UDP service
     */
    public static <T> RpcUdpSpider<T> buildSpider(TypeReference<T> clazz, RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, SimpleChannelInboundHandler<RpcUdpPacket<T>> simpleChannelInboundHandler) {
        return new RpcUdpSpider<>(rpcEventLoopManager, channelOptions, new ChannelInitializer<>() {
            @Override
            protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                SimpleChannelInboundHandler<DatagramPacket> decoder = new SimpleChannelInboundHandler<>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                        T decode = ByteBufUtil.decode(datagramPacket.content(), clazz);
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
            throw new RpcException(RpcErrorEnum.CONNECT, "repeat bindings");
        }
        Bootstrap b = new Bootstrap();
        b.group(rpcEventLoopManager.getEventLoopGroup());
        b.channel(rpcEventLoopManager.getDatagramChannelClass());
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
        // Indicates that a port is randomly bind
        return bind(0);
    }


    public ChannelFuture close() {
        if (this.channel != null) {
            return channel.close();
        }
        return null;
    }

    public void sendMsg(T msg, InetSocketAddress to) {
        sendGeneralMsg(this.channel, msg, to);
    }

    public static <T> void sendGeneralMsg(Channel channel, T msg, InetSocketAddress to) {
        RpcMsgTransManager.sendUdpMsg(channel, msg, to);
    }

}
