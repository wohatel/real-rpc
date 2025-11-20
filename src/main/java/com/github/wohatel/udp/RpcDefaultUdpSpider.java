package com.github.wohatel.udp;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcUdpEventLoopManager;
import com.github.wohatel.interaction.common.RpcUdpPacket;
import com.github.wohatel.interaction.common.RpcUdpWaiter;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A default implementation of UDP spider for RPC communication.
 * This class provides basic functionality for sending and receiving RPC messages over UDP.
 */
@Slf4j
public class RpcDefaultUdpSpider {

    /**
     * The underlying RPC UDP spider instance that handles the actual network communication.
     */
    @Getter
    protected final RpcUdpSpider<RpcRequest> rpcUdpSpider;

    /**
     * A consumer for handling received RPC messages.
     * It takes a ChannelHandlerContext and an RpcUdpPacket as input.
     */
    protected BiConsumer<RpcUdpWaiter<RpcRequest>, RpcUdpPacket<RpcRequest>> rpcMsgConsumer;

    /**
     * An inbound handler for processing incoming RPC UDP packets.
     * It extends SimpleChannelInboundHandler to handle RpcUdpPacket<RpcRequest> messages.
     */
    @Getter
    protected SimpleChannelInboundHandler<RpcUdpPacket<RpcRequest>> inbondHandler = new SimpleChannelInboundHandler<>() {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<RpcRequest> packet) throws Exception {
            // If a message consumer is set, accept and process the incoming packet
            if (rpcMsgConsumer != null) {
                rpcMsgConsumer.accept(new RpcUdpWaiter<>(channelHandlerContext), packet);
            }
        }
    };

    /**
     * Default constructor that uses the default event loop manager.
     */
    public RpcDefaultUdpSpider() {
        this(RpcUdpEventLoopManager.of());
    }

    /**
     * Sets the message consumer for handling received RPC messages.
     *
     * @param rpcMsgConsumer The consumer that will handle incoming messages
     */
    public void onMsgReceive(BiConsumer<RpcUdpWaiter<RpcRequest>, RpcUdpPacket<RpcRequest>> rpcMsgConsumer) {
        this.rpcMsgConsumer = rpcMsgConsumer;
    }

    /**
     * Constructor that uses a specified event loop manager.
     * @param eventLoopManager rpcEventLoopManager
     */
    public RpcDefaultUdpSpider(RpcUdpEventLoopManager eventLoopManager) {
        this(eventLoopManager, null, null);
    }

    /**
     *
     * @param eventLoopManager rpcEventLoopManager
     * @param channelOptions Connection channel options
     */
    public RpcDefaultUdpSpider(RpcUdpEventLoopManager eventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, BiConsumer<RpcUdpWaiter<RpcRequest>, RpcUdpPacket<RpcRequest>> udpMsgConsumer) {
        this.rpcMsgConsumer = udpMsgConsumer;
        this.rpcUdpSpider = RpcUdpSpider.buildSpider(new TypeReference<RpcRequest>() {
        }, eventLoopManager, channelOptions, inbondHandler);
    }

    /**
     * Bind the port that the UDP service starts
     */
    public ChannelFuture bind(int port) {
        return rpcUdpSpider.bind(port);
    }

    /**
     * Turn off the udp service
     */
    public ChannelFuture close() {
        return rpcUdpSpider.close();
    }

    /**
     * send msg to remote address
     *
     * @param rpcRequest request
     * @param to         target socket address
     */
    public void sendMsg(RpcRequest rpcRequest, InetSocketAddress to) {
        rpcUdpSpider.sendMsg(rpcRequest, to);
    }

}
