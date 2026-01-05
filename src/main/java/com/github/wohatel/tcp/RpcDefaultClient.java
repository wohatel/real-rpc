package com.github.wohatel.tcp;

import com.alibaba.fastjson2.JSONObject;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.initializer.RpcMsgChannelInitializer;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.ChannelOptionAndValue;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.common.RpcSocketEventLoopManager;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.interaction.constant.RpcSessionType;
import com.github.wohatel.interaction.file.RpcFileSenderInput;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.tcp.builder.RpcClientConnectConfig;
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetSocketAddress;


/**
 * Default implementation of RPC client that extends RpcDataReceiver.
 * Provides functionality for connecting to RPC servers, sending requests, and managing sessions.
 */
@Slf4j
public class RpcDefaultClient extends RpcDataReceiver {
    @Getter
    protected final RpcClientConnectConfig connectConfig;
    @Getter
    protected final RpcSocketEventLoopManager eventLoopManager;

    public RpcDefaultClient(RpcClientConnectConfig config, RpcSocketEventLoopManager eventLoopManager) {
        super(new RpcMsgChannelInitializer(config.getRpcVivoHandler()));
        this.connectConfig = config;
        this.eventLoopManager = eventLoopManager;
    }

    @Override
    public final void onSessionRequestReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        throw new RpcException(RpcErrorEnum.RUNTIME, "onSessionMsgReceive is unavailable in client");
    }

    @SuppressWarnings("all")
    public ChannelFuture connect() {
        if (this.channel != null && this.channel.isActive()) {
            throw new RpcException(RpcErrorEnum.CONNECT, "the connection is alive:RpcDefaultClient");
        }
        Bootstrap b = new Bootstrap();
        b.group(eventLoopManager.getEventLoopGroup());
        b.channel(eventLoopManager.getChannelClass());
        b.handler(rpcMsgChannelInitializer);
        b.option(ChannelOption.TCP_NODELAY, true);
        if (!EmptyVerifyUtil.isEmpty(connectConfig.getChannelOptions())) {
            for (ChannelOptionAndValue channelOption : connectConfig.getChannelOptions()) {
                b.option(channelOption.getChannelOption(), channelOption.getValue());
            }
        }
        InetSocketAddress remote = InetSocketAddress.createUnresolved(connectConfig.getHost(), connectConfig.getPort());
        ChannelFuture f = connectConfig.getLocalAddress() == null ? b.connect(remote) : b.connect(remote, connectConfig.getLocalAddress());
        this.channel = f.channel();
        f.addListener(future -> {
            if (future.isSuccess()) {
                log.info("connect to-{}:{} success", connectConfig.getHost(), connectConfig.getPort());
            } else {
                log.error("connect to-{}:{} failure", connectConfig.getHost(), connectConfig.getPort());
            }
        });
        return f;
    }

    public void sendFile(File file, RpcFileSenderInput input) {
        RpcMsgTransManager.sendFile(channel, file, input);
    }


    public void interruptSendFile(RpcSession rpcSession) {
        RpcMsgTransManager.interruptSendFile(this.channel, rpcSession);
    }

    public void sendRequest(RpcRequest rpcRequest) {
        RpcMsgTransManager.sendRequest(channel, rpcRequest);
    }

    public RpcFuture sendSynRequest(RpcRequest rpcRequest) {
        return this.sendSynRequest(rpcRequest, RpcNumberConstant.OVER_TIME);
    }

    public RpcFuture sendSynRequest(RpcRequest rpcRequest, long timeOut) {
        return RpcMsgTransManager.sendSynRequest(channel, rpcRequest, timeOut);
    }

    public void sendSessionRequest(RpcSessionRequest rpcSessionRequest) {
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        RpcSessionFuture sessionFuture = RpcFutureTransManager.getSessionFuture(rpcSession.getSessionId());
        if (sessionFuture == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the session does not exist, try opening a new one");
        }
        if (sessionFuture.getRpcSessionProcess() == RpcSessionProcess.FINISHED) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the session is over, try opening a new one");
        }
        rpcSessionRequest.setSessionProcess(RpcSessionProcess.RUNNING);
        RpcFutureTransManager.verifySessionRequest(rpcSessionRequest);
        RpcMsgTransManager.sendRequest(channel, rpcSessionRequest);
    }

    /**
     * Establish a session
     *
     * @param rpcSession rpcSession
     * @return RpcSessionFuture
     */
    public RpcSessionFuture startSession(RpcSession rpcSession) {
        return startSession(rpcSession, null);
    }

    /**
     * Establish a session with context
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
        rpcRequest.setSessionProcess(RpcSessionProcess.TOSTART);
        if (context != null) {
            rpcRequest.setBody(JSONObject.toJSONString(context));
        }
        RpcSessionFuture rpcFuture = RpcFutureTransManager.verifySessionRequest(rpcRequest);
        rpcFuture.setRpcSessionProcess(RpcSessionProcess.TOSTART);
        rpcFuture.setRpcSessionType(RpcSessionType.session);
        RpcMsgTransManager.sendRequest(channel, rpcRequest);
        RpcReaction rpcReaction = rpcFuture.get();
        if (rpcReaction.isSuccess()) {
            rpcFuture.setRpcSessionProcess(RpcSessionProcess.RUNNING);
        } else {
            RpcFutureTransManager.remove(rpcSession.getSessionId());
        }
        return rpcFuture;
    }

    /**
     * close session
     */
    public void stopSession(RpcSession rpcSession) {
        RpcSessionFuture sessionFuture = RpcFutureTransManager.getSessionFuture(rpcSession.getSessionId());
        if (sessionFuture == null || sessionFuture.getRpcSessionProcess() == RpcSessionProcess.FINISHED) {
            return;
        }
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
        rpcRequest.setSessionProcess(RpcSessionProcess.FINISHED);
        rpcRequest.setNeedReaction(false);
        RpcFutureTransManager.stopSessionGracefully(rpcSession.getSessionId());
        RpcMsgTransManager.sendRequest(channel, rpcRequest);
    }
}
