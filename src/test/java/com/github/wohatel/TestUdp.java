package com.github.wohatel;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.udp.RpcUdpPacket;
import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;


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
        RpcUdpSpider<List<RpcRequest>> server = RpcUdpSpider.buildSpider(new TypeReference<List<RpcRequest>>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<List<RpcRequest>>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<List<RpcRequest>> datagramPacket) throws Exception {
                List<RpcRequest> msg = datagramPacket.getMsg();
                System.out.println("几个:" + msg.size());
                System.out.println("返回的时候,给加一个null");
                msg.add(null);
                // 回复
                RpcUdpSpider.sendGeneralMsg(channelHandlerContext.channel(), msg, datagramPacket.getSender());
            }
        });
        server.bind(8765).sync();


        RpcUdpSpider<List<RpcRequest>> client = RpcUdpSpider.buildSpider(new TypeReference<List<RpcRequest>>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<List<RpcRequest>>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<List<RpcRequest>> datagramPacket) throws Exception {
                List<RpcRequest> msg = datagramPacket.getMsg();

                System.out.println("服务端返回:" + msg.size());
            }
        });

        client.bind().sync();
        client.sendMsg(List.of(RpcRequest.withBody("你好")), new InetSocketAddress("127.0.0.1", 8765));
        Thread.currentThread().join();
    }

}
