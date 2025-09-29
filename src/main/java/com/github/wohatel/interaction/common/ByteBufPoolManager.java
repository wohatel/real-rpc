package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.util.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
public class ByteBufPoolManager {

    private static final SessionManager<ByteBufPool> SESSION_MANAGER = new SessionManager<>(NumberConstant.TEN_EIGHT_K, (sessionId, bytePoll) -> {
        bytePoll.destory();
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
            buf.release();
            throw new RpcException(RpcErrorEnum.RUNTIME, "session is not exist");
        }
        SESSION_MANAGER.flushTime(sessionId);
        SESSION_MANAGER.getSession(sessionId).release(buf);
    }

    public static void destory(String sessionId) {
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
         * Get a ByteBuf from the pool and throw an exception
         * if it can't be obtained within the specified time
         */
        public ByteBuf borrow(long timeoutMillis) throws TimeoutException, InterruptedException {
            ByteBuf buf = pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (buf == null) {
                log.error("borrow timed out waiting for available ByteBuf from pool");
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