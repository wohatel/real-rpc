package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.util.ReferenceByteBufUtil;
import com.github.wohatel.util.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class ByteBufPoolManager {

    private static final SessionManager<ByteBufPool> SESSION_MANAGER = new SessionManager<>(NumberConstant.TEN_EIGHT_K, (sessionId, bytePoll) -> {
        bytePoll.destroy();
    });

    public static void init(String sessionId, int poolSize, int chunkSize) {
        if (SESSION_MANAGER.contains(sessionId)) {
            log.error("byteBufPool session is exists");
            throw new RpcException(RpcErrorEnum.RUNTIME, "byteBufPool session is exists");
        }
        SESSION_MANAGER.initSession(sessionId, new ByteBufPool(poolSize, chunkSize));
    }

    public static void release(String sessionId, ByteBuf buf) {
        if (buf == null) return;
        if (!SESSION_MANAGER.contains(sessionId)) {
            log.error("release session is not exist");
            // 优先释放buf
            ReferenceByteBufUtil.safeRelease(buf);
            throw new RpcException(RpcErrorEnum.RUNTIME, "session is not exist");
        }
        SESSION_MANAGER.flushTime(sessionId);
        SESSION_MANAGER.getSession(sessionId).release(buf);
    }

    public static void destroy(String sessionId) {
        SESSION_MANAGER.releaseAndSessionClose(sessionId);
    }

    public static ByteBuf borrow(String sessionId, Long timeOut) throws InterruptedException, TimeoutException {
        if (!SESSION_MANAGER.contains(sessionId)) {
            log.error("borrow session is not exist");
            throw new RpcException(RpcErrorEnum.RUNTIME, "session is not exist");
        }
        SESSION_MANAGER.flushTime(sessionId);
        return SESSION_MANAGER.getSession(sessionId).borrow(timeOut);
    }

    private static class ByteBufPool {
        private final BlockingQueue<ByteBuf> pool;
        private final Map<Integer, ByteBuf> byteBufMap;
        private final int chunkSize;
        private final AtomicBoolean released = new AtomicBoolean(false);

        public ByteBufPool(int poolSize, int chunkSize) {
            this.pool = new ArrayBlockingQueue<>(poolSize);
            this.byteBufMap = new ConcurrentHashMap<>(NumberConstant.TEN);
            this.chunkSize = chunkSize;
            for (int i = 0; i < poolSize; i++) {
                ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.directBuffer(chunkSize);
                pool.add(buf);
            }
        }

        /**
         * Get a ByteBuf from the pool and throw an exception
         * if it can't be obtained within the specified time
         */
        public ByteBuf borrow(long timeoutMillis) throws TimeoutException, InterruptedException {
            ByteBuf buf = pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (buf == null) {
                log.error("borrow timed out waiting for available ByteBuf from pool");
                throw new TimeoutException("timed out waiting for available ByteBuf from pool");
            }
            byteBufMap.put(System.identityHashCode(buf), buf);
            return buf;
        }

        /**
         * 归还buf
         *
         * @param buf 内存
         */
        public void release(ByteBuf buf) {
            if (buf == null) return;
            if (byteBufMap.remove(System.identityHashCode(buf)) != null) {
                buf.clear();
                if (!released.get()) {
                    pool.add(buf);
                }
            } else {
                log.warn("attempted to release a ByteBuf that was not borrowed from pool");
            }
        }

        /**
         * 销毁
         */
        public void destroy() {
            if (released.compareAndSet(false, true)) {
                ByteBuf buf;
                while ((buf = pool.poll()) != null) {
                    ReferenceByteBufUtil.safeRelease(buf);
                }
                byteBufMap.forEach((key, value) -> ReferenceByteBufUtil.safeRelease(value));
                byteBufMap.clear();
            }
        }
    }
}