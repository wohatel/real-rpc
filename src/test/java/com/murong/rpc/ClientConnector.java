package com.murong.rpc;


import com.murong.rpc.client.Pool;
import io.netty.channel.Channel;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.Future;

import java.util.concurrent.ExecutionException;

public class ClientConnector {

    public void test() throws ExecutionException, InterruptedException {
        Pool pool = new Pool(1);
        FixedChannelPool channelPool = pool.getPool(8888);

        for (int i = 0; i < 1; i++) {
            Future<Channel> acquire = channelPool.acquire();
            Channel channel = acquire.get();
            channel.writeAndFlush("good" + i);
        }

    }
}
