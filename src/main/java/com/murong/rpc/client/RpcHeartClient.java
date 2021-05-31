package com.murong.rpc.client;

import com.murong.rpc.client.handler.RpcClientHeartHandler;
import io.netty.channel.Channel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;


public class RpcHeartClient extends SimpleRpcClient {

    public RpcHeartClient(Channel channel) {
        this.channel = channel;
        this.channel.pipeline()
                .addLast(new IdleStateHandler(60, 15, 0, TimeUnit.SECONDS))
                .addLast(new RpcClientHeartHandler(this));
    }

}
