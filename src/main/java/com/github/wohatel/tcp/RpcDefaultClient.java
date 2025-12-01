package com.github.wohatel.tcp;

import com.alibaba.fastjson2.JSONObject;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
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
import com.github.wohatel.util.EmptyVerifyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;


/**
 * Default implementation of RPC client that extends RpcDataReceiver.
 * Provides functionality for connecting to RPC servers, sending requests, and managing sessions.
 */
@Slf4j
public class RpcDefaultClient extends RpcDataReceiver {

    /**
     * Manager for handling RPC event loops
     */
    @Getter
    protected final RpcSocketEventLoopManager eventLoopManager;
    /**
     * List of channel options and their values for configuring the connection
     */
    @Getter
    protected final List<ChannelOptionAndValue<Object>> channelOptions;
    // If you need to bind a local NIC to connect to remote services, you need to set it
    @Getter
    @Setter
    protected SocketAddress localAddress;

    public RpcDefaultClient(String host, int port) {
        this(host, port, RpcSocketEventLoopManager.of());
    }

    public RpcDefaultClient(String host, int port, RpcSocketEventLoopManager socketEventLoopManager) {
        this(host, port, socketEventLoopManager, null);
    }

    public RpcDefaultClient(String host, int port, RpcSocketEventLoopManager socketEventLoopManager, List<ChannelOptionAndValue<Object>> channelOptions) {
        super(host, port);
        this.eventLoopManager = socketEventLoopManager;
        this.channelOptions = channelOptions;
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
                log.info("connect to-{}:{} success", host, port);
            } else {
                log.error("connect to-{}:{} failure", host, port);
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
