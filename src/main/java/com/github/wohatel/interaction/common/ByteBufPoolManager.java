package com.github.wohatel.interaction.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
public class ByteBufPoolManager {

    private static final Map<String, ByteBufPool> POOL_MAP = new ConcurrentHashMap<>();

    public static void init(String sessionId, int poolSize, int chunkSize) {
        POOL_MAP.computeIfAbsent(sessionId, id -> new ByteBufPool(poolSize, chunkSize));
    }

    public static boolean isFull(String sessionId) {
        ByteBufPool pool = POOL_MAP.get(sessionId);
        if (pool == null) {
            return false;
        }
        return pool.isFull();
    }

    public static void destory(String sessionId) {
        ByteBufPool pool = POOL_MAP.remove(sessionId);
        if (pool != null) {
            pool.destory();
        }
    }


    public static void release(String sessionId, ByteBuf buf) {
        if (buf != null) {
            ByteBufPool pool = POOL_MAP.get(sessionId);
            if (pool != null) {
                pool.release(buf);
            } else {
                if (buf.refCnt() > 0) {
                    buf.release();
                }
            }
        }
    }

    public static ByteBuf borrow(String sessionId, Long timeOut) throws InterruptedException, TimeoutException {
        ByteBufPool pool = POOL_MAP.get(sessionId);
        return pool.borrow(timeOut);
    }

    private static class ByteBufPool {
        private final BlockingQueue<ByteBuf> pool;
        private final List<ByteBuf> list;

        public ByteBufPool(int poolSize, int chunkSize) {
            this.pool = new ArrayBlockingQueue<>(poolSize);
            list = new ArrayList<>(poolSize);
            for (int i = 0; i < poolSize; i++) {
                ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.directBuffer(chunkSize);
                pool.add(buf);
                list.add(buf);
            }
        }

        /**
         * 从池中获取 ByteBuf，如果在指定时间内无法获取，则抛出异常
         */
        public ByteBuf borrow(long timeoutMillis) throws TimeoutException, InterruptedException {
            ByteBuf buf = pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (buf == null) {
                throw new TimeoutException("timed out waiting for available ByteBuf from pool");
            }
            return buf;
        }

        public void release(ByteBuf buf) {
            if (buf != null) {
                buf.clear(); // 重置写指针
                pool.offer(buf); // 放回池中（不会阻塞）
            }
        }

        public boolean isFull() {
            return list.size() == pool.size();
        }

        public void destory() {
            for (ByteBuf byteBuf : list) {
                try {
                    if (byteBuf != null && byteBuf.refCnt() > 0) {
                        byteBuf.release();
                    }
                } catch (Exception e) {
                    log.error("destroy anomalies:", e);
                }
            }
            list.clear();
            pool.clear();
        }
    }

}