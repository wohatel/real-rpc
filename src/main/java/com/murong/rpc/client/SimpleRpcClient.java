package com.murong.rpc.client;

import com.alibaba.fastjson.JSON;

import com.murong.rpc.client.handler.RpcClientRequestHandler;
import com.murong.rpc.client.handler.RpcMessageClientInteractionHandler;
import com.murong.rpc.interaction.*;
import io.netty.channel.Channel;

import java.io.IOException;

public class SimpleRpcClient {

    protected Channel channel;

    public SimpleRpcClient() {
        this(true);
    }

    public SimpleRpcClient(boolean callGc) {
        if (callGc) {
            RpcGc.callWake();
        }
    }

    public void closeChannel() {
        if (channel != null) {
            channel.close();
        }
    }

    public void initRpcHandler() {
        this.channel.pipeline().addLast(new RpcMessageClientInteractionHandler());
        this.channel.pipeline().addLast(new RpcClientRequestHandler());
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     * 设置channel
     *
     * @param channel
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
        initRpcHandler();
    }

    public void sendMsg(RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            return;
        }
        if (this.channel == null || !this.channel.isActive()) {
            throw new RuntimeException("连接不可用");
        }
        RpcMsgTransUtil.sendMsg(channel,rpcRequest);
    }

    public RpcFuture sendSynMsg(RpcRequest rpcRequest) {
        RpcFuture rpcFuture = RpcInteractionContainer.addRequest(rpcRequest);
        rpcRequest.setNeedResponse(true);
        this.sendMsg(rpcRequest);
        return rpcFuture;
    }

    public RpcFuture sendSynMsg(RpcRequest rpcRequest, long timeOut) {
        RpcFuture rpcFuture = RpcInteractionContainer.addRequest(rpcRequest, timeOut);
        rpcRequest.setNeedResponse(true);
        this.sendMsg(rpcRequest);
        return rpcFuture;
    }

    public void sendFile(String file) throws IOException {
        RpcMsgTransUtil.writeFile(channel, file);
    }
}
