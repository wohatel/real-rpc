package com.murong.rpc.client;


import com.murong.rpc.initializer.StringChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class Pool {
    // key为目标host，value为目标host的连接池
    private ChannelPoolMap<InetSocketAddress, FixedChannelPool> poolMap;


    public FixedChannelPool getPool(InetSocketAddress address) {
        return poolMap.get(address);
    }

    public FixedChannelPool getPool(int port) {
        return this.getPool(new InetSocketAddress(port));
    }

    public FixedChannelPool getPool(String hostname, int port) {
        return this.getPool(new InetSocketAddress(hostname, port));
    }

    public Pool(int maxConnections) {
        init(maxConnections);
    }

    private void init(final int maxConnections) {


        ChannelPoolHandler handler = new ChannelPoolHandler() {
            /**
             * 使用完channel需要释放才能放入连接池
             */
            @Override
            public void channelReleased(Channel ch) throws Exception {
                // 刷新管道里的数据

            }

            /**
             * 当链接创建的时候添加channelhandler，只有当channel不足时会创建，但不会超过限制的最大channel数
             */
            @Override
            public void channelCreated(Channel ch) throws Exception {
                ch.pipeline().addLast(new StringChannelInitializer());
            }

            /**
             * 获取连接池中的channel
             */
            @Override
            public void channelAcquired(Channel ch) throws Exception {

            }
        };

        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true);

        //所有的公用一个eventloopgroup, 对于客户端来说应该问题不大!
        poolMap = new AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool>() {
            @Override
            protected FixedChannelPool newPool(InetSocketAddress key) {
                return new FixedChannelPool(bootstrap.remoteAddress(key), handler, maxConnections);
            }
        };

    }
}