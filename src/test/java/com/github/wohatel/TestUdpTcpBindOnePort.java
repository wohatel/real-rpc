package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.udp.RpcUdpPacket;
import com.github.wohatel.udp.RpcUdpSpider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;


/**
 * description
 *
 */
public class TestUdpTcpBindOnePort {

    /**
     * server 病定了8760, 但是spiderA 想向网段内的所有人统统发送通知
     */
    @Test
    void testBroadCast() throws InterruptedException {
        // udp绑定8765
        RpcUdpSpider<String> server = RpcUdpSpider.buildStringSpider(new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("udp服务端收到:" + datagramPacket.getMsg());
            }
        });
        server.bind(8765).sync();

        // tcp绑定8765
        MultiThreadIoEventLoopGroup eventExecutors = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        RpcServer rpcServer = new RpcServer(8765, eventExecutors, eventExecutors);
        rpcServer.onMsgReceive(new RpcSimpleRequestMsgHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                System.out.println("tcp服务端收到:" + request.getBody());
            }
        });
        rpcServer.start().sync();

        // udpc-lient
        RpcUdpSpider<String> client = RpcUdpSpider.buildStringSpider(new SimpleChannelInboundHandler<RpcUdpPacket<String>>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcUdpPacket<String> datagramPacket) throws Exception {
                System.out.println("client收到消息:" + datagramPacket.getMsg());
            }
        });
        client.bind().sync();

        // tcp-client
        RpcDefaultClient rpcDefaultClient = new RpcDefaultClient("127.0.0.1", 8765, eventExecutors);
        rpcDefaultClient.connect().sync();


        client.sendMsg("udp客户端发送第一次请求", new InetSocketAddress("127.0.0.1", 8765));
        rpcDefaultClient.sendMsg(RpcRequest.withBody("tcp客户端发送第一次请求"));


        Thread.currentThread().join();
    }

}
