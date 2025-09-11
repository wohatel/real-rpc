package com.github.wohatel;


import com.github.wohatel.tcp.RpcAutoReconnectClient;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;

public class ReconnectClientTest {

    /**
     * 断线重连测试
     * 1: 开启server服务
     * 2: 开启client
     * 3: 计时3s后,client的链接
     * 5: 此时显示断连,但很快重连上
     */
    public static void main(String[] args) throws InterruptedException {
        serverStart();
        RpcAutoReconnectClient test = test();

        Thread.sleep(3000);
        test.getChannel().close();


        Thread.sleep(3000);
        test.sendMsg(new RpcRequest());
        test.sendMsg(new RpcRequest());
    }


    public static RpcServer serverStart() {
        RpcServer rpcServer = new RpcServer(8765,new NioEventLoopGroup(),new NioEventLoopGroup());
        rpcServer.start();
        rpcServer.onMsgReceive(new RpcSimpleRequestMsgHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                System.out.println(request.getRequestId());
            }
        });
        return rpcServer;
    }

    public static RpcAutoReconnectClient test() {
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8765, new NioEventLoopGroup());
        client.autoReconnect();
        return client;
    }


}
