package com.murong.rpc.tcp;


import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.file.RpcFileSenderInput;
import com.murong.rpc.interaction.common.RpcInteractionContainer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;


/**
 * @author yaochuang
 */
@Slf4j
public class RpcDefaultClient extends AbstractRpcClient {
    @Getter
    protected final String host;
    @Getter
    protected final Integer port;
    protected final MultiThreadIoEventLoopGroup eventLoopGroup;
    protected final Class<? extends Channel> channelClass;

    public RpcDefaultClient(String host, int port, MultiThreadIoEventLoopGroup eventLoopGroup) {
        this.host = host;
        this.port = port;
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = getChannelClass();
        this.rpcMsgChannelInitializer = new RpcMsgChannelInitializer();
    }

    public ChannelFuture connect() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RuntimeException("不支持多次链接:RpcDefaultClient");
        }
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup);
        b.channel(channelClass).option(ChannelOption.TCP_NODELAY, true);
        b.handler(rpcMsgChannelInitializer);
        ChannelFuture f = b.connect(host, port);
        f.addListener(future -> {
            if (future.isSuccess()) {
                initClient(f.channel());
                log.info("链接to-" + host + ":" + port + "成功");
            } else {
                log.error("链接to-" + host + ":" + port + "失败");
            }
        });
        return f;
    }

    public void sendFile(File file, RpcFileSenderInput input) {
        RpcMsgTransUtil.writeFile(channel, file, input);
    }

    /**
     * 强制中断传输可能引发异常操作,谨慎使用
     */
    public void interruptSendFile(RpcSession rpcSession) {
        RpcMsgTransUtil.writeStopFile(this.channel, rpcSession);
    }

    public void sendMsg(RpcRequest rpcRequest) {
        RpcMsgTransUtil.sendMsg(channel, rpcRequest);
    }

    public RpcFuture sendSynMsg(RpcRequest rpcRequest) {
        return this.sendSynMsg(rpcRequest, NumberConstant.OVER_TIME);
    }

    public RpcFuture sendSynMsg(RpcRequest rpcRequest, long timeOut) {
        return RpcMsgTransUtil.sendSynMsg(channel, rpcRequest, timeOut);
    }

    public void sendSessionMsg(RpcSessionRequest rpcSessionRequest) {
        if (!RpcInteractionContainer.contains(rpcSessionRequest.getRpcSession().getSessionId())) {
            throw new RuntimeException("会话不存在,请先构建会话");
        }
        RpcMsgTransUtil.sendSessionRequest(channel, rpcSessionRequest);
    }

    /**
     * 建立会话
     *
     * @param rpcSession rpcSession
     * @return RpcSessionFuture
     */
    public RpcSessionFuture startSession(RpcSession rpcSession) {
        return RpcMsgTransUtil.sendSessionStartRequest(channel, rpcSession);
    }

    /**
     * 建立会话
     *
     * @param rpcSession session
     * @return RpcSessionFuture
     */
    public RpcSessionFuture startSession(RpcSession rpcSession, RpcSessionContext context) {
        return RpcMsgTransUtil.sendSessionStartRequest(channel, rpcSession, context);
    }

    /**
     * 关闭会话
     */
    public void finishSession(RpcSession rpcSession) {
        RpcMsgTransUtil.sendSessionFinishRequest(channel, rpcSession);
    }

    /**
     * 获取session内置对象
     *
     * @param rpcSession session
     * @return RpcSessionFuture
     */
    public RpcSessionFuture getSessionFuture(RpcSession rpcSession) {
        return RpcInteractionContainer.getSessionFuture(rpcSession.getSessionId());
    }

    /**
     * 返回类型
     */
    protected Class<? extends Channel> getChannelClass() {
        if (this.eventLoopGroup.isIoType(NioIoHandler.class)) {
            return NioSocketChannel.class;
        }
        if (this.eventLoopGroup.isIoType(EpollIoHandler.class)) {
            return EpollSocketChannel.class;
        }
        if (this.eventLoopGroup.isIoType(KQueueIoHandler.class)) {
            return KQueueSocketChannel.class;
        }
        if (this.eventLoopGroup.isIoType(LocalIoHandler.class)) {
            return LocalChannel.class;
        }
        throw new RuntimeException("eventLoopGroup 类型暂时不支持");
    }
}
