package com.murong.rpc.client;


import com.murong.rpc.initializer.RpcMsgChannelInitializer;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.NoShutNioEventLoopGroup;
import com.murong.rpc.interaction.file.RpcFileSenderInput;
import com.murong.rpc.interaction.common.RpcInteractionContainer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.File;


/**
 * @author yaochuang
 */
@Log
public class RpcDefaultClient extends AbstractRpcClient {
    @Getter
    protected String host;
    @Getter
    protected Integer port;
    protected NioEventLoopGroup nioEventLoopGroup;

    public RpcDefaultClient(String host, int port, NioEventLoopGroup nioEventLoopGroup) {
        this.host = host;
        this.port = port;
        this.nioEventLoopGroup = nioEventLoopGroup;
        this.rpcMsgChannelInitializer = new RpcMsgChannelInitializer();
    }

    public RpcDefaultClient(String host, int port) {
        this(host, port, NoShutNioEventLoopGroup.acquireNioEventLoopGroup());
    }

    public ChannelFuture connect() {
        if (this.channel != null) {
            throw new RuntimeException("不支持多次链接:RpcDefaultClient");
        }
        Bootstrap b = new Bootstrap();
        b.group(nioEventLoopGroup);
        b.channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true);
        b.handler(rpcMsgChannelInitializer);
        ChannelFuture f = b.connect(host, port);
        f.addListener(new GenericFutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    initClient(f.channel());
                    log.info("链接to-" + host + ":" + port + "成功");
                } else {
                    log.warning("链接to-" + host + ":" + port + "失败");
                }
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
     * @param rpcSession
     * @return
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
     * @param rpcSession
     * @param rpcSession
     * @return
     */
    public RpcSessionFuture getSessionFuture(RpcSession rpcSession) {
        return RpcInteractionContainer.getSessionFuture(rpcSession.getSessionId());
    }

}
