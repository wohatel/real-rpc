package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.util.FlushStrategy;
import com.github.wohatel.util.ReferenceByteBufUtil;
import com.github.wohatel.util.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A manager class for ByteBuf pools that provides session-based buffer management.
 * This class implements a thread-safe pool of ByteBuf objects that can be borrowed and returned
 * by different sessions, with automatic cleanup of resources when sessions are destroyed.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ByteBufPoolManager {

    // Session manager for handling ByteBuf pools with a capacity of 10^8
    // Each session has its own ByteBuf pool with a cleanup callback that destroys the pool when the session is released
    private static final SessionManager<ByteBufPool> SESSION_MANAGER = new SessionManager<>(RpcNumberConstant.K_TEN_EIGHT, (sessionId, bytePoll) -> bytePoll.destroy(), FlushStrategy.buildDefault(RpcNumberConstant.K_TEN_EIGHT));

    /**
     * Initialize a new ByteBuf pool for the given session.
     *
     * @param sessionId The identifier for the session
     * @param poolSize  The number of ByteBuf objects in the pool
     * @param chunkSize The size of each ByteBuf in bytes
     * @throws RpcException If a pool already exists for the given session
     */
    public static void init(String sessionId, int poolSize, int chunkSize) {
        if (SESSION_MANAGER.contains(sessionId)) {
            log.error("byteBufPool session is exists");
            throw new RpcException(RpcErrorEnum.RUNTIME, "byteBufPool session is exists");
        }
        SESSION_MANAGER.initSession(sessionId, new ByteBufPool(poolSize, chunkSize));
    }

    /**
     * Release a ByteBuf back to its session's pool.
     *
     * @param sessionId The identifier for the session
     * @param buf       The ByteBuf to release
     * @throws RpcException If the session doesn't exist
     */
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

    /**
     * Destroys the session associated with the given session ID.
     * This method will release the session resources and close the session.
     *
     * @param sessionId the ID of the session to be destroyed
     */
    public static void destroy(String sessionId) {
        // Release session resources and close the session using SESSION_MANAGER
        SESSION_MANAGER.releaseAndSessionClose(sessionId);
    }

    /**
     * Borrows a ByteBuf from a session with the given session ID and timeout.
     *
     * @param sessionId The ID of the session to borrow from
     * @param timeOut   The timeout period for borrowing
     * @return The borrowed ByteBuf
     * @throws InterruptedException If the thread is interrupted while waiting
     * @throws TimeoutException     If the borrowing operation times out
     */
    public static ByteBuf borrow(String sessionId, Long timeOut) throws InterruptedException, TimeoutException {
        // Check if the session exists in the session manager
        if (!SESSION_MANAGER.contains(sessionId)) {
            // Log error and throw exception if session doesn't exist
            log.error("borrow session is not exist");
            throw new RpcException(RpcErrorEnum.RUNTIME, "session is not exist");
        }
        // Update the session's last access time
        SESSION_MANAGER.flushTime(sessionId);
        // Borrow and return the ByteBuf from the session
        return SESSION_MANAGER.getSession(sessionId).borrow(timeOut);
    }

    /**
     * A thread-safe pool for managing ByteBuf instances to reduce allocation overhead.
     * This pool uses a blocking queue for available buffers and a map to track borrowed buffers.
     */
    private static class ByteBufPool {
        // Queue to store available ByteBuf instances
        private final BlockingQueue<ByteBuf> pool;
        // Map to track borrowed ByteBuf instances
        private final Map<Integer, ByteBuf> byteBufMap;
        // Atomic flag to indicate if the pool has been released
        private final AtomicBoolean released = new AtomicBoolean(false);

        /**
         * Constructs a new ByteBufPool with specified size and chunk size.
         *
         * @param poolSize  The maximum number of ByteBuf instances in the pool
         * @param chunkSize The size of each ByteBuf in bytes
         */
        public ByteBufPool(int poolSize, int chunkSize) {
            this.pool = new ArrayBlockingQueue<>(poolSize);
            this.byteBufMap = new ConcurrentHashMap<>(RpcNumberConstant.TEN);
            // Initialize the pool with direct ByteBuf instances
            for (int i = 0; i < poolSize; i++) {
                ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.directBuffer(chunkSize);
                pool.add(buf);
            }
        }

        /**
         * Get a ByteBuf from the pool and throw an exception
         * if it can't be obtained within the specified time
         *
         * @param timeoutMillis Maximum time to wait for an available ByteBuf
         * @return A borrowed ByteBuf instance
         * @throws TimeoutException     If the timeout expires before a ByteBuf is available
         * @throws InterruptedException If the thread is interrupted while waiting
         */
        public ByteBuf borrow(long timeoutMillis) throws TimeoutException, InterruptedException {
            // Try to get a ByteBuf from the pool with timeout
            ByteBuf buf = pool.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            if (buf == null) {
                log.error("borrow timed out waiting for available ByteBuf from pool");
                throw new TimeoutException("timed out waiting for available ByteBuf from pool");
            }
            // Track the borrowed buffer
            byteBufMap.put(System.identityHashCode(buf), buf);
            return buf;
        }


        /**
         * Releases a ByteBuf back to the pool if it was borrowed from it
         *
         * @param buf The ByteBuf to be released
         */
        public void release(ByteBuf buf) {
            // If the buffer is null, do nothing
            if (buf == null) return;
            // Try to remove the buffer from the tracking map using its identity hash code
            if (byteBufMap.remove(System.identityHashCode(buf)) != null) {
                // Clear the buffer before returning it to the pool
                buf.clear();
                // Check if the pool is still active (not released)
                if (!released.get()) {
                    // Add the buffer back to the pool for reuse
                    pool.add(buf);
                }
            } else {
                // Log a warning if someone tries to release a buffer that wasn't borrowed from the pool
                log.warn("attempted to release a ByteBuf that was not borrowed from pool");
            }
        }


        /**
         * Destroys the pool and releases all ByteBuf resources.
         * This method ensures that all ByteBuf instances in the pool are safely released.
         * It uses a compare-and-set operation to ensure thread-safe execution only once.
         */
        public void destroy() {
            // Check and update the release status using atomic compare-and-set operation
            if (released.compareAndSet(false, true)) {
                ByteBuf buf;
                // Poll and release all ByteBuf instances from the pool
                while ((buf = pool.poll()) != null) {
                    ReferenceByteBufUtil.safeRelease(buf);
                }
                // Release all ByteBuf instances in the byteBufMap
                byteBufMap.forEach((key, value) -> ReferenceByteBufUtil.safeRelease(value));
                // Clear the byteBufMap after releasing all resources
                byteBufMap.clear();
            }
        }
    }
}