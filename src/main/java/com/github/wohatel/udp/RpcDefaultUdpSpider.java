package com.github.wohatel.udp;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * default proxy udp spider class, used for forwarding and receiving requests
 *
 * @author yaochuang 2025/09/28 09:44
 */
@Slf4j
public class RpcDefaultUdpSpider {

    @Getter
    protected final RpcUdpSpider<RpcRequest> rpcUdpSpider;

    protected BiConsumer<ChannelHandlerContext, RpcUdpPacket<RpcRequest>> rpcMsgConsumer;

    @Getter
    protected SimpleChannelInboundHandler<RpcUdpPacket<RpcRequest>> inbondHandler = new SimpleChannelInboundHandler<>() {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<RpcRequest> packet) throws Exception {
            if (rpcMsgConsumer != null) {
                rpcMsgConsumer.accept(channelHandlerContext, packet);
            }
        }
    };

    public RpcDefaultUdpSpider() {
        this(RpcEventLoopManager.ofDefault());
    }

    public void onMsgReceive(BiConsumer<ChannelHandlerContext, RpcUdpPacket<RpcRequest>> rpcMsgConsumer) {
        this.rpcMsgConsumer = rpcMsgConsumer;
    }

    /**
     *
     * @param rpcEventLoopManager rpcEventLoopManager
     */
    public RpcDefaultUdpSpider(RpcEventLoopManager rpcEventLoopManager) {
        this(rpcEventLoopManager, null, null);
    }

    /**
     *
     * @param rpcEventLoopManager rpcEventLoopManager
     * @param channelOptions Connection channel options
     */
    public RpcDefaultUdpSpider(RpcEventLoopManager rpcEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions, BiConsumer<ChannelHandlerContext, RpcUdpPacket<RpcRequest>> udpMsgConsumer) {
        this.rpcMsgConsumer = udpMsgConsumer;
        this.rpcUdpSpider = RpcUdpSpider.buildSpider(new TypeReference<RpcRequest>() {
        }, rpcEventLoopManager, channelOptions, inbondHandler);
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
