package com.github.wohatel;

import com.alibaba.fastjson2.TypeReference;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.common.RpcEventLoopManager;
import com.github.wohatel.interaction.common.RpcReactionWaiter;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.interaction.common.RpcUdpPacket;
import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;


/**
 *
 */
public class TestUdpTcpBindOnePort {

    /**
     * server 病定了8760, 但是spiderA 想向网段内的所有人统统发送通知
     */
    @Test
    void testBroadCast() throws InterruptedException {
        // udp绑定8765
        RpcUdpSpider<String> server = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("udp服务端收到:" + datagramPacket.getMsg());
            }
        });
        server.bind(8765).sync();

        RpcEventLoopManager eventLoopManager = RpcEventLoopManager.of(new NioEventLoopGroup());
        // tcp绑定8765
        RpcServer rpcServer = new RpcServer(8765, eventLoopManager);
        rpcServer.onRequestReceive(new RpcSimpleRequestMsgHandler() {
            @Override
            public void onReceiveRequest(RpcRequest request, RpcReactionWaiter waiter) {
                System.out.println("tcp服务端收到:" + request.getBody());
            }
        });
        rpcServer.start().sync();

        // udpc-lient
        RpcUdpSpider<String> client = RpcUdpSpider.buildSpider(new TypeReference<String>() {
        }, new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("client收到消息:" + datagramPacket.getMsg());
            }
        });
        client.bind().sync();

        // tcp-client
        RpcDefaultClient rpcDefaultClient = new RpcDefaultClient("127.0.0.1", 8765, eventLoopManager);
        rpcDefaultClient.connect().sync();


        client.sendMsg("udp客户端发送第一次请求", new InetSocketAddress("127.0.0.1", 8765));
        rpcDefaultClient.sendRequest(RpcRequest.withBody("tcp客户端发送第一次请求"));


        Thread.currentThread().join();
    }

}
