package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import com.github.wohatel.util.LinkedNode;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import io.netty.channel.nio.NioEventLoopGroup;
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


        LinkedNode<String, ChannelHandler> initChannelHandlers = server.getRpcMsgChannelInitializer().getInitChannelHandlers();
        LinkedNode<String, ChannelHandler> msgHandler = initChannelHandlers.findFirst("msgHandler");
        // 在server启动之前预先定义处理器链路(也可对client预先定义)
        // 在消息接收之前添加一个handler一旦收到消息就打印
        // "----------新的handler--------"
        msgHandler.addBefore(LinkedNode.build("activeHandler", adapter));

        // 等待服务端开启成功
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
