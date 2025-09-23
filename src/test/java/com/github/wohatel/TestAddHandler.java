package com.github.wohatel;

import com.github.wohatel.decoder.RpcMsgCompressDecoder;
import com.github.wohatel.decoder.RpcMsgCompressEncoder;
import com.github.wohatel.decoder.RpcMsgDecoder;
import com.github.wohatel.decoder.RpcMsgEncoder;
import com.github.wohatel.initializer.RpcMessageBaseInquiryHandler;
import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.util.LinkedNode;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.junit.jupiter.api.Test;

/**
 *
 *
 * @author yaochuang 2025/09/11 16:55
 */
public class TestAddHandler {

    private static RpcServer server;
    private static RpcDefaultClient client;
    private static NioEventLoopGroup group;

    private static ChannelInboundHandlerAdapter adapter = new ChannelInboundHandlerAdapter() {
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("channel active");
            ctx.fireChannelActive();
        }

        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("channel channelInactive");
            ctx.fireChannelInactive();
        }

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("----------新的handler--------");
            ctx.fireChannelRead(msg);
        }
    };


    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {

        // 线程组暂时用一个
        group = new NioEventLoopGroup();
        server = new RpcServer(8765, group, group);


        // 在server启动之前预先定义处理器链路(也可对client预先定义)
        // 在消息接收之前添加一个handler一旦收到消息就打印
        // "----------新的handler--------"


        // 等待服务端开启成功


        RpcMsgChannelInitializer rpcMsgChannelInitializer = new RpcMsgChannelInitializer() {

            public void init(SocketChannel socketChannel) {
                socketChannel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                pipeline.addLast("decompress", new RpcMsgCompressDecoder());
                pipeline.addLast("compress", new RpcMsgCompressEncoder());
                pipeline.addLast("decoder", new RpcMsgDecoder());
                pipeline.addLast("encoder", new RpcMsgEncoder());
                pipeline.addLast("baseHandler", new RpcMessageBaseInquiryHandler());


                pipeline.addLast("actived", adapter);


                pipeline.addLast("msgHandler", this.getRpcMessageInteractionHandler());
            }

        };

        server.setRpcMsgChannelInitializer(rpcMsgChannelInitializer);
        server.start().sync();


        client = new RpcDefaultClient("127.0.0.1", 8765, group);


        // 等待客户端连接成功
        client.connect().sync();


        // 绑定服务端接收消息处理
        server.onMsgReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        // 客户度发送消息
        client.sendMsg(RpcRequest.withBody("hello ketty"));

        /**
         * 也可在逻辑
         * client.getChannel().pipeline().addAfter()
         */

        // 防止线程退出
        Thread.currentThread().join();
    }


}
