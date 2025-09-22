package com.github.wohatel.tcp;


import com.alibaba.fastjson2.JSONObject;
import com.github.wohatel.constant.RpcBaseAction;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileSenderInput;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalIoHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;


/**
 * @author yaochuang
 */
@Slf4j
public class RpcDefaultClient extends RpcDataReceiver {

    protected final MultiThreadIoEventLoopGroup eventLoopGroup;
    protected final Class<? extends Channel> channelClass;
    protected final List<ChannelOptionAndValue<Object>> channelOptions;
    // 如需要绑定本地网卡去连接远程服务需要set
    @Getter
    @Setter
    protected SocketAddress localAddress;


    public RpcDefaultClient(String host, int port, MultiThreadIoEventLoopGroup eventLoopGroup) {
        this(host, port, eventLoopGroup, null);
    }

    public RpcDefaultClient(String host, int port, MultiThreadIoEventLoopGroup eventLoopGroup, List<ChannelOptionAndValue<Object>> channelOptions) {
        super(host, port);
        this.eventLoopGroup = eventLoopGroup;
        this.channelClass = getChannelClass();
        this.channelOptions = channelOptions;
    }

    @Override
    public final void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        throw new RpcException(RpcErrorEnum.RUNTIME, "onSessionMsgReceive is unavailable in client");
    }

    @SuppressWarnings("all")
    public ChannelFuture connect() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "the connection is alive:RpcDefaultClient");
        }
        Bootstrap b = new Bootstrap();
        b.group(eventLoopGroup);
        b.channel(channelClass);
        b.handler(rpcMsgChannelInitializer);
        if (!EmptyVerifyUtil.isEmpty(channelOptions)) {
            for (ChannelOptionAndValue channelOption : channelOptions) {
                b.option(channelOption.getChannelOption(), channelOption.getValue());
            }
        }
        InetSocketAddress remote = InetSocketAddress.createUnresolved(host, port);
        ChannelFuture f = localAddress == null ? b.connect(remote) : b.connect(remote, localAddress);
        this.channel = f.channel();
        f.addListener(future -> {
            if (future.isSuccess()) {
                log.info("connect to-" + host + ":" + port + " success");
            } else {
                log.error("connect to-" + host + ":" + port + "failure");
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
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the session does not exist, try opening a new one");
        }
        if (!channel.id().asShortText().equals(sessionFuture.getChannelId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "session doesn't match the session");
        }
        if (sessionFuture.isSessionFinish()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the session is over, try opening a new one");
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
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession the logo cannot be empty");
        }
        if (RpcFutureTransManager.contains(rpcSession.getSessionId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the session already exists and cannot be opened repeatedly");
        }
        if (RpcSessionTransManger.isRunning(rpcSession.getSessionId())) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the session already exists and is opened by the remote end");
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
            throw new RpcException(RpcErrorEnum.SEND_MSG, "session doesn't match the session");
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
        throw new RpcException(RpcErrorEnum.RUNTIME, "eventLoopGroup types are not supported at the moment");
    }

}
