package com.murong.rpc;

import com.murong.rpc.udp.RpcUdpSpider;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;


/**
 * description
 *
 * @author yaochuang 2025/09/09 15:06
 */
public class UdpTest {

    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
        RpcUdpSpider server = new RpcUdpSpider(eventExecutors, new SimpleChannelInboundHandler<DatagramPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                InetSocketAddress sender = datagramPacket.sender();
                String msg = datagramPacket.content().toString(CharsetUtil.UTF_8);
                System.out.println(msg);
                DatagramPacket response = new DatagramPacket(Unpooled.copiedBuffer("返回数据".getBytes()), sender);
                channelHandlerContext.writeAndFlush(response);
            }
        });
        server.bind(8765);
        Thread.sleep(1000);


        RpcUdpSpider client = new RpcUdpSpider(eventExecutors, new SimpleChannelInboundHandler<DatagramPacket>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                System.out.println("客户端接收0");


                String msg = datagramPacket.content().toString(CharsetUtil.UTF_8);

                System.out.println("接收到:" + msg);

                System.out.println("客户端接收1");
            }
        });

        client.bindAsClient();
        Thread.sleep(1000);

        client.sendMsg("发送", new InetSocketAddress("127.0.0.1", 8765));


        Thread.sleep(10000);


    }

}
