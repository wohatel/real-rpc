package com.murong.rpc.udp;

import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;

/**
 * @author yaochuang
 */
public class RpcUdpSpider {
    private final NioEventLoopGroup nioEventLoopGroup;
    private final SimpleChannelInboundHandler<DatagramPacket> clientChannelHandler;
    private Channel channel;

    public RpcUdpSpider(NioEventLoopGroup nioEventLoopGroup, SimpleChannelInboundHandler<DatagramPacket> clientChannelHandler) {
        this.nioEventLoopGroup = nioEventLoopGroup;
        this.clientChannelHandler = clientChannelHandler;
    }

    @SneakyThrows
    public void bind(int port) {
        if (this.channel != null && this.channel.isActive()) {
            return;
        }
        Bootstrap b = new Bootstrap();
        b.group(nioEventLoopGroup);
        b.channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, true);
        b.handler(this.clientChannelHandler);
        this.channel = b.bind(port).sync().channel();
    }

    @SneakyThrows
    public void bindAsClient() {
        // 表示随机绑定一个端口,主要是客户端向服务端发送数据
        bind(0);
    }

    /**
     * 发送消息
     */
    public void sendMsg(String msg, InetSocketAddress to) {
        RpcMsgTransUtil.sendUdpMsg(this.channel, msg, to);
    }

}
