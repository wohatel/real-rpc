package com.github.wohatel.tcp;


import com.alibaba.fastjson2.JSONObject;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.constant.RpcBaseAction;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.file.RpcFileSenderInput;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.constant.NumberConstant;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;


/**
 * @author yaochuang
 */
@Slf4j
public class RpcDefaultClient extends RpcDataReceiver {
    @Getter
    protected final String host;
    @Getter
    protected final Integer port;
    protected final MultiThreadIoEventLoopGroup eventLoopGroup;
    protected final Class<? extends Channel> channelClass;


    public RpcDefaultClient(String host, int port, MultiThreadIoEventLoopGroup eventLoopGroup) {
        super(true);
        this.host = host;
        this.port = port;
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = getChannelClass();
    }

    public ChannelFuture connect() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "连接存活中:RpcDefaultClient");
        }
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup);
        b.channel(channelClass).option(ChannelOption.TCP_NODELAY, true);
        b.handler(rpcMsgChannelInitializer);
        ChannelFuture f = b.connect(host, port);
        this.channel = f.channel();
        f.addListener(future -> {
            if (future.isSuccess()) {
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
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        RpcSessionFuture sessionFuture = RpcFutureTransManager.getSessionFuture(rpcSession.getSessionId());
        if (sessionFuture == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "会话不存在,请尝试开启新的会话");
        }
        if (!channel.id().asShortText().equals(sessionFuture.getChannelId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "session 与会话不匹配");
        }
        if (sessionFuture.isSessionFinish()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "会话已结束,请尝试开启新的会话");
        }

        rpcSessionRequest.setSessionProcess(RpcSessionProcess.ING);
        RpcFutureTransManager.verifySessionRequest(rpcSessionRequest);
        RpcMsgTransUtil.sendMsg(channel, rpcSessionRequest);
    }

    /**
     * 建立会话
     *
     * @param rpcSession rpcSession
     * @return RpcSessionFuture
     */
    public RpcSessionFuture startSession(RpcSession rpcSession) {
        return startSession(rpcSession, null);
    }

    /**
     * 询问会话是否存在
     *
     * @param rpcSession rpcSession
     * @return RpcSessionFuture
     */
    public boolean inquiryServerSession(RpcSession rpcSession) {
        if (!RpcFutureTransManager.contains(rpcSession.getSessionId())) {
            return false;
        }
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestType(RpcBaseAction.BASE_INQUIRY_SESSION.name());
        rpcRequest.setBody(rpcSession.getSessionId());
        RpcFuture rpcFuture = RpcMsgTransUtil.sendSynMsg(channel, rpcRequest);
        RpcResponse rpcResponse = rpcFuture.get();
        return rpcResponse.isSuccess();
    }

    /**
     * 询问会话是否存在
     *
     * @return RpcSessionFuture
     */
    public String inquiryServerNodeId() {
        return RpcMsgTransUtil.sendInquiryRemoteNodeIdRequest(channel);
    }

    /**
     * 建立会话
     *
     * @param rpcSession session
     * @return RpcSessionFuture
     */
    public RpcSessionFuture startSession(RpcSession rpcSession, RpcSessionContext context) {
        if (rpcSession == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession标识不能为空");
        }
        if (RpcFutureTransManager.contains(rpcSession.getSessionId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "会话已存在,不可重复开启");
        }
        if (RpcSessionTransManger.isRunning(rpcSession.getSessionId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "会话已存在,已由远端开启");
        }
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
        rpcRequest.setSessionProcess(RpcSessionProcess.START);
        rpcRequest.setOrigin(channel.id().asShortText());
        if (context != null) {
            rpcRequest.setBody(JSONObject.toJSONString(context));
        }
        RpcSessionFuture rpcFuture = RpcFutureTransManager.verifySessionRequest(rpcRequest);
        rpcFuture.setChannelId(channel.id().asShortText());
        RpcMsgTransUtil.sendMsg(channel, rpcRequest);
        RpcResponse rpcResponse = rpcFuture.get();
        if (rpcResponse.isSuccess()) {
            rpcFuture.setRpcSessionProcess(RpcSessionProcess.ING);
        }
        return rpcFuture;
    }

    /**
     * 关闭会话
     */
    public void finishSession(RpcSession rpcSession) {
        RpcSessionFuture sessionFuture = RpcFutureTransManager.getSessionFuture(rpcSession.getSessionId());
        if (sessionFuture == null || sessionFuture.isSessionFinish()) {
            return;
        }
        if (!channel.id().asShortText().equals(sessionFuture.getChannelId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "session 与会话不匹配");
        }
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
        rpcRequest.setSessionProcess(RpcSessionProcess.FiNISH);
        rpcRequest.setNeedResponse(false);
        RpcFutureTransManager.stopSessionGracefully(rpcSession.getSessionId());
        RpcMsgTransUtil.sendMsg(channel, rpcRequest);
    }

    /**
     * 获取session内置对象
     *
     * @param rpcSession session
     * @return RpcSessionFuture
     */
    public RpcSessionFuture getSessionFuture(RpcSession rpcSession) {
        return RpcFutureTransManager.getSessionFuture(rpcSession.getSessionId());
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
        throw new RpcException(RpcErrorEnum.RUNTIME, "eventLoopGroup 类型暂时不支持");
    }

}
