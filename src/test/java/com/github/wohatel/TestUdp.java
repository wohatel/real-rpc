package com.github.wohatel;

import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;


/**
 * description
 *
 * @author yaochuang 2025/09/09 15:06
 */
public class TestUdp {

    /**
     * udp 一般作为信条使用,比如每隔3s发送一次消息给到服务端,服务端如果连续多次不应答,就认为断联
     */
    @Test
    void testUdp() throws InterruptedException {
        RpcUdpSpider server = RpcUdpSpider.buildSimpleSpider(new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                InetSocketAddress sender = datagramPacket.sender();
                String msg = datagramPacket.content().toString(CharsetUtil.UTF_8);
                System.out.println(msg);
                DatagramPacket response = new DatagramPacket(Unpooled.copiedBuffer("返回数据".getBytes()), sender);
                channelHandlerContext.writeAndFlush(response);
            }
        });
        server.bind(8765).sync();


        RpcUdpSpider client = RpcUdpSpider.buildSimpleSpider(new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                System.out.println("客户端接收0");
                String msg = datagramPacket.content().toString(CharsetUtil.UTF_8);
                System.out.println("接收到:" + msg);
                System.out.println("客户端接收1");
            }
        });

        client.bindAsClient().sync();
        client.sendMsg("发送", new InetSocketAddress("127.0.0.1", 8765));
        Thread.currentThread().join();
    }

}
