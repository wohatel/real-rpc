package com.murong.rpc.interaction.common;

import com.murong.rpc.client.AbstractRpcClient;
import io.netty.channel.EventLoopTaskQueueFactory;
import io.netty.channel.SelectStrategyFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.RejectedExecutionHandler;

import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * description
 *
 * @author yaochuang 2025/05/07 12:43
 */
public class NoShutNioEventLoopGroup extends NioEventLoopGroup {

    private static volatile NioEventLoopGroup nioEventLoopGroup;

    public NoShutNioEventLoopGroup() {
        this(0, (Executor)null);
    }

    public NoShutNioEventLoopGroup(int nThreads) {
        this(nThreads, (Executor)null);
    }

    public NoShutNioEventLoopGroup(ThreadFactory threadFactory) {
        super(0, (ThreadFactory)threadFactory, SelectorProvider.provider());
    }

    public NoShutNioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory, SelectorProvider.provider());
    }

    public NoShutNioEventLoopGroup(int nThreads, Executor executor) {
        super(nThreads, executor, SelectorProvider.provider());
    }

    public NoShutNioEventLoopGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory, SelectorProvider selectorProvider, SelectStrategyFactory selectStrategyFactory, RejectedExecutionHandler rejectedExecutionHandler, EventLoopTaskQueueFactory taskQueueFactory) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler, taskQueueFactory);
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return null;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public List<Runnable> shutdownNow() {
        return new ArrayList<>();
    }

    public static NioEventLoopGroup acquireNioEventLoopGroup() {
        if (nioEventLoopGroup == null) { // 第一次检查
            synchronized (AbstractRpcClient.class) {
                if (nioEventLoopGroup == null) { // 第二次检查
                    // 进制关停连接池
                    nioEventLoopGroup = new NoShutNioEventLoopGroup();
                }
            }
        }
        return nioEventLoopGroup;
    }
}
