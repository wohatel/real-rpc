package com.github.wohatel;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.udp.RpcUdpPacket;
import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;


/**
 *
 * @author yaochuang 2025/09/09 15:06
 */
public class TestUdpString {

    /**
     * udp 一般作为信条使用,比如每隔3s发送一次消息给到服务端,服务端如果连续多次不应答,就认为断联
     */
    @Test
    void testUdp() throws InterruptedException {
        RpcUdpSpider<String> server = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                // 回复
                RpcUdpSpider.sendGeneralMsg(channelHandlerContext.channel(), "你也好", datagramPacket.getSender());
            }
        });
        server.bind(8765).sync();


        RpcUdpSpider<String> client = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("服务端的问候:" + datagramPacket.getMsg());
                System.out.println(datagramPacket.getMsg());
            }
        });

        client.bind().sync();
        client.sendMsg("你好", new InetSocketAddress("127.0.0.1", 8765));
        Thread.currentThread().join();
    }

}
