package com.github.wohatel;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.udp.RpcUdpPacket;
import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;


/**
 * description
 *
 */
public class TestUdpBroadCast {

    /**
     * server 病定了8760, 但是spiderA 想向网段内的所有人统统发送通知
     */
    @Test
    void testBroadCast() throws InterruptedException {
        RpcUdpSpider<String> server = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("服务端收到回复:" + datagramPacket.getMsg());
            }
        });
        server.bind(8765).sync();


        RpcUdpSpider<String> client1 = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("client1收到广播消息:" + datagramPacket.getMsg());
                RpcUdpSpider.sendGeneralMsg(channelHandlerContext.channel(), "张三", datagramPacket.getSender());
            }
        });

        RpcUdpSpider<String> client2 = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("client2收到广播消息:" + datagramPacket.getMsg());
                RpcUdpSpider.sendGeneralMsg(channelHandlerContext.channel(), "张三", datagramPacket.getSender());
            }
        });

        client1.bind(8888).sync();
        client2.bind(7777).sync();

        // 注意此处的地址为 255.255.255.255, 会向网段内所有绑定8888的端口的发送udp消息
        // client2收不到消息是因为绑定的是7777, 正式情况下,一般集群中的所有udp会绑定同一个的端口
        server.sendMsg("收到请报名", new InetSocketAddress("255.255.255.255", 8888));
        Thread.currentThread().join();
    }

}
