package com.murong.rpc.client;

import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.common.NoShutNioEventLoopGroup;
import com.murong.rpc.interaction.handler.RpcHeartTimeOutHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class RpcHeartClient extends AbstractRpcClient {
    private final AtomicBoolean alived = new AtomicBoolean(false);

    private final IdleStateHandler idleStateHandler;

    private final RpcHeartTimeOutHandler rpcHeartTimeOutHandler;
    private final String host;
    private final long timeOutMillis;
    private final int port;

    /**
     * @param host                   链接地址
     * @param port                   端口
     * @param timeOutMillis          超过此事件没有通信认为超时
     * @param rpcHeartTimeOutHandler 超时后的处理
     */
    public RpcHeartClient(String host, int port, long timeOutMillis, RpcHeartTimeOutHandler rpcHeartTimeOutHandler) {
        if (timeOutMillis <= 0) {
            throw new RuntimeException("超时时间配置异常");
        }
        this.host = host;
        this.port = port;
        this.timeOutMillis = timeOutMillis;
        this.rpcMsgChannelInitializer = new RpcMsgChannelInitializer();
        long timeBy3 = timeOutMillis / 3;
        long interval; // 心跳探查间隔
        if (timeBy3 >= 3000L) {
            interval = 3000L;
        } else if (timeBy3 > 2000L) {
            interval = 2000L;
        } else if (timeBy3 > 1000L) {
            interval = 1000L;
        } else if (timeBy3 > 500L) {
            interval = 500L;
        } else {
            interval = timeOutMillis / 2 + 1;
        }
        this.idleStateHandler = new IdleStateHandler(timeOutMillis, interval, timeOutMillis, TimeUnit.MILLISECONDS);
        this.rpcHeartTimeOutHandler = rpcHeartTimeOutHandler;
    }

    public RpcHeartClient(String host, int port, long timeOutMillis) {
        this(host, port, timeOutMillis, null);
    }

    public RpcHeartClient(String host, int port) {
        this(host, port, NumberConstant.K_ONE);
    }

    public ChannelFuture connect() {
        if (this.channel != null) {
            throw new RuntimeException("不支持多次链接:RpcDefaultClient");
        }
        Bootstrap b = new Bootstrap();
        b.group(NoShutNioEventLoopGroup.acquireNioEventLoopGroup());
        b.channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
        b.handler(this.rpcMsgChannelInitializer);
        ChannelFuture connect = b.connect(host, port);
        connect.addListener(new GenericFutureListener<>() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    initClient(connect.channel());
                    alived.set(true);
                    connect.channel().pipeline().addLast(idleStateHandler).addLast(new RpcClientHeartHandler(rpcHeartTimeOutHandler, alived));
                }
            }
        });
        return connect;
    }

    @SneakyThrows
    public static boolean testConnect(String host, int port) {
        return testConnect(host, port, 500L);
    }

    @SneakyThrows
    public static boolean testConnect(String host, int port, long timeOutMillis) {
        RpcHeartClient heartClient = new RpcHeartClient(host, port, timeOutMillis);
        try {
            heartClient.connect();
            Thread.sleep(timeOutMillis);
            return heartClient.isAlived();
        } finally {
            heartClient.close();
        }
    }

    public boolean isAlived() {
        return alived.get();
    }

    /**
     * 心跳读取消息
     *
     * @author murong 2018-08-03
     * @version 1.0
     */
    @AllArgsConstructor
    public static class RpcClientHeartHandler extends ChannelInboundHandlerAdapter {

        private final RpcHeartTimeOutHandler rpcHeartTimeOutHandler;
        private final AtomicBoolean alived;

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            IdleStateEvent idle = (IdleStateEvent) evt;
            if (idle.state() == IdleState.WRITER_IDLE) {
                RpcMsgTransUtil.sendHeart(ctx.channel());
            } else if (idle.state() == IdleState.READER_IDLE) {
                // 此时说明心跳已经超时
                alived.set(false);
                if (rpcHeartTimeOutHandler != null) {
                    rpcHeartTimeOutHandler.channelInactive(ctx);
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            alived.set(false);
            if (rpcHeartTimeOutHandler != null) {
                rpcHeartTimeOutHandler.channelInactive(ctx);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            RpcMsgTransUtil.sendHeart(ctx.channel());
        }

    }
}
