package com.github.wohatel.udp;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.interaction.common.RpcUdpPacket;
import com.github.wohatel.util.ByteBufUtil;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import lombok.Data;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * A generic UDP spider implementation for RPC communication.
 * This class provides functionality to create, bind, and manage UDP channels
 * for sending and receiving RPC messages over UDP protocol.
 *
 * @param <T> The type of message payload to be sent and received
 */
@Data
public class RpcUdpSpider<T> {
    private static final Logger logger = LoggerFactory.getLogger(RpcUdpSpider.class);
    // Manager for RPC event loops
    protected final RpcEventLoopManager rpcEventLoopManager;
    // Initializer for configuring channels
    protected final ChannelInitializer<DatagramChannel> channelInitializer;
    // The active channel for communication
    protected Channel channel;
    // List of channel options and their values
    protected final List<ChannelOptionAndValue<Object>> channelOptions;

    /**
     * Constructor with default channel options
     *
     * @param rpcEventLoopManager The event loop manager for handling RPC events
     * @param channelInitializer  The initializer for configuring channels
     */
    public RpcUdpSpider(RpcEventLoopManager rpcEventLoopManager, ChannelInitializer<DatagramChannel> channelInitializer) {
        this(rpcEventLoopManager, null, channelInitializer);
    }

    /**
     * Full constructor with all parameters
     *
     * @param rpcEventLoopManager The event loop manager for handling RPC events
     * @param channelOptions      List of channel options and their values
     * @param channelInitializer  The initializer for configuring channels
     */
    public RpcUdpSpider(RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, ChannelInitializer<DatagramChannel> channelInitializer) {
        this.rpcEventLoopManager = rpcEventLoopManager;
        this.channelInitializer = channelInitializer;
        this.channelOptions = channelOptions;
    }

    /**
     * Builds a simple UDP spider with default event loop manager
     *
     * @param clazz                       Type reference for the message type
     * @param simpleChannelInboundHandler Handler for processing incoming messages
     * @return A new RpcUdpSpider instance
     */
    public static <T> RpcUdpSpider<T> buildSpider(TypeReference<T> clazz, SimpleChannelInboundHandler<RpcUdpPacket<T>> simpleChannelInboundHandler) {
        return buildSpider(clazz, RpcEventLoopManager.ofDefault(), null, simpleChannelInboundHandler);
    }

    /**
     * Build a simple UDP service
     * @param clazz Type reference for the message type
     * @param rpcEventLoopManager The event loop manager for handling RPC events
     * @param channelOptions List of channel options and their values
     * @param simpleChannelInboundHandler Handler for processing incoming messages
     * @return A new RpcUdpSpider instance
     */
    public static <T> RpcUdpSpider<T> buildSpider(TypeReference<T> clazz, RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, SimpleChannelInboundHandler<RpcUdpPacket<T>> simpleChannelInboundHandler) {
        return new RpcUdpSpider<>(rpcEventLoopManager, channelOptions, new ChannelInitializer<>() {
            @Override
            protected void initChannel(DatagramChannel datagramChannel) throws Exception {
                // Create a decoder to handle incoming datagram packets
                SimpleChannelInboundHandler<DatagramPacket> decoder = new SimpleChannelInboundHandler<>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                        // Decode the message content
                        T decode = ByteBufUtil.decode(datagramPacket.content(), clazz);
                        // Create RPC UDP packet
                        RpcUdpPacket<T> rpcUdpPacket = new RpcUdpPacket<>();
                        rpcUdpPacket.setMsg(decode);
                        rpcUdpPacket.setSender(datagramPacket.sender());
                        rpcUdpPacket.setRecipient(datagramPacket.recipient());
                        // Forward the decoded packet to the next handler in the pipeline
                        channelHandlerContext.fireChannelRead(rpcUdpPacket);
                    }
                };
                // Add decoder and message handler to the channel pipeline
                datagramChannel.pipeline().addLast(decoder).addLast(simpleChannelInboundHandler);
            }
        });
    }

    /**
     * Binds the UDP spider to a specific port
     *
     * @param port The port number to bind to
     * @return ChannelFuture representing the bind operation
     * @throws RpcException if attempting to bind to an already active channel
     */
    @SneakyThrows
    @SuppressWarnings("all")
    public ChannelFuture bind(int port) {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "udp repeat bindings");
        }
        // Create and configure the bootstrap
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
