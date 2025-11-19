package com.github.wohatel.util;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.common.RpcSessionFlushStrategy;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * SessionManager class for managing sessions with automatic cleanup and timeout handling.
 *
 * @author yaochuang 2025/04/22 18:57
 */
@Data
@Slf4j
public class SessionManager<T> {
    // Session timeout duration in milliseconds
    private final Long sessionTime;
    // Atomic flag to indicate if the session manager is stopped
    private final AtomicBoolean stop = new AtomicBoolean(false);
    // Concurrent map to store active sessions
    private final Map<String, T> container = new ConcurrentHashMap<>();
    // Map to store release callbacks for sessions
    private final Map<String, Consumer<T>> onRelease = new ConcurrentHashMap<>();
    // Map to store expiration times for sessions
    private final Map<String, AtomicLong> timeFlushMap = new ConcurrentHashMap<>();
    // Priority queue for delayed cleanup of expired sessions
    private final DelayQueue<DelayItem> delayQueue = new DelayQueue<>();
    // Background thread for cleaning up expired sessions
    private final Thread cleanerThread;
    // Callback to be executed when a session is closed
    private final BiConsumer<String, T> sessionClose;

    /**     
     * Refresh factor (if 0.4)
     * If the timeout corresponding to a request is 10s, if the distance from the timeout is > 4s, it will not be refreshed, and it will be refreshed if it is less than 4s.
     * That is, the higher this value, the more frequently it is refreshed
     * If the refresh factor is negative, refresh immediately
     */
    private final double flushSeed;

    /**
     * Constructor for SessionManager with default flush seed
     *
     * @param sessionTime  The duration of a session in milliseconds
     * @param sessionClose Callback to execute when a session is closed
     */
    public SessionManager(long sessionTime, BiConsumer<String, T> sessionClose) {
        this(sessionTime, sessionClose, 0.618);
    }

    /**
     * Constructor for SessionManager with custom flush seed
     *
     * @param sessionTime  The duration of a session in milliseconds
     * @param sessionClose Callback to execute when a session is closed
     * @param flushSeed    Factor determining when to refresh session timeouts
     */
    public SessionManager(long sessionTime, BiConsumer<String, T> sessionClose, double flushSeed) {
        this.flushSeed = flushSeed;
        this.sessionTime = sessionTime;
        if (sessionTime <= 0) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "session time error: session time <= 0");
        }
        this.sessionClose = sessionClose;
        cleanerThread = Thread.startVirtualThread(this::cleanerLoop);
    }

    /**
     * Constructor for SessionManager without session close callback
     *
     * @param sessionTime The duration of a session in milliseconds
     */
    public SessionManager(long sessionTime) {
        this(sessionTime, null);
    }


    /**     
     * 初始化资源
     *
     * @param sessionId sessionId
     * @param resource  资源
     */
    public void initSession(String sessionId, T resource) {
        this.initSession(sessionId, resource, null);
    }

    /**     
     * 初始化资源
     *
     * @param sessionId sessionId
     * @param resource  资源
     */
    public void initSession(String sessionId, T resource, Long expiredAt) {
        if (!stop.get()) {
            T old = container.putIfAbsent(sessionId, resource);
            if (old == null) {
                long finalExpiredAt = expiredAt == null ? System.currentTimeMillis() + sessionTime : expiredAt;
                timeFlushMap.put(sessionId, new AtomicLong(finalExpiredAt));
                delayQueue.add(new DelayItem(sessionId, finalExpiredAt));
            } else {
                throw new RpcException(RpcErrorEnum.RUNTIME, "session already exists:" + sessionId);
            }
        }
    }

    /**
     * 注册销毁事件
     *
     * @param sessionId sessionId
     * @param consumer  消费者
     */
    public void registOnRelease(String sessionId, Consumer<T> consumer) {
        if (consumer != null && container.containsKey(sessionId)) {
            onRelease.put(sessionId, consumer);
        }
    }

    @SneakyThrows
    public T release(String sessionId) {
        timeFlushMap.remove(sessionId);
        T remove = container.remove(sessionId);
        Consumer<T> consumer = onRelease.remove(sessionId);
        if (remove != null && consumer != null) {
            DefaultVirtualThreadPool.execute(() -> consumer.accept(remove));
        }
        return remove;
    }

    @SneakyThrows
    public void releaseAndSessionClose(String sessionId) {
        T release = release(sessionId);
        if (this.sessionClose != null && release != null) {
            sessionClose.accept(sessionId, release);
        }
    }

    @SneakyThrows
    public boolean contains(String sessionId) {
        return container.containsKey(sessionId);
    }

    /**     
     * 获取session
     *
     * @param sessionId 获取session的资源
     * @return 返回session的资源
     */
    public T getSession(String sessionId) {
        return container.get(sessionId);
    }

    /**     
     * 获取session
     *
     * @param sessionId 获取session的资源
     * @return 返回session的资源
     */
    public Long getExpireAt(String sessionId) {
        AtomicLong atomicLong = timeFlushMap.get(sessionId);
        if (atomicLong == null) {
            return null;
        }
        return atomicLong.get();
    }

    public int sessionSize() {
        return container.size();
    }

    /**     
     * 关闭管理器
     */
    public void destroy() {
        this.stop.set(true);
        cleanerThread.interrupt();
        try {
            cleanerThread.join();  // 等线程真的退出，保证干净关闭
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.timeFlushMap.clear();
        for (String sessionId : new ArrayList<>(container.keySet())) {
            try {
                this.releaseAndSessionClose(sessionId);
            } catch (Exception e) {
                log.error("destroy Manager exception:", e);
            }
        }
    }

    /**     
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下session 的最近交互时间
     */
    public boolean flushTime(String sessionId, long sessionTime, boolean force) {

        if (!stop.get() && container.containsKey(sessionId)) {
            AtomicLong atomicLong = timeFlushMap.get(sessionId);
            if (force || RpcSessionFlushStrategy.isNeedFlushForExpired(atomicLong.get(), sessionTime, this.flushSeed)) {
                long expiredAt = System.currentTimeMillis() + sessionTime;
                delayQueue.add(new DelayItem(sessionId, expiredAt));
                atomicLong.set(expiredAt);
            }
            return true;
        }
        return false;
    }

    /**     
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下会话的最近交互时间
     */
    public boolean flushTime(String sessionId, long sessionTime) {
        return flushTime(sessionId, sessionTime, false);
    }

    /**     
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下会话的最近交互时间
     */
    public boolean flushTime(String sessionId) {
        return this.flushTime(sessionId, sessionTime);
    }

    /**     
     * 循环清理
     */
    private void cleanerLoop() {
        while (!stop.get()) {
            try {
                DelayItem item = delayQueue.take();
                T resource = container.get(item.sessionId);
                if (resource == null) {// 说明已经被移除
                    continue;
                }
                AtomicLong expireAt = timeFlushMap.get(item.sessionId);
                long now = System.currentTimeMillis();
                if (now >= expireAt.get()) {
                    DefaultVirtualThreadPool.execute(() -> releaseAndSessionClose(item.sessionId));
                } else {
                    delayQueue.offer(new DelayItem(item.sessionId, expireAt.get()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("cleanerLoop exception:", ex);
            }
        }
    }

    private static class DelayItem implements Delayed {
        private final String sessionId;
        private final long expireAt;

        public DelayItem(String sessionId, long expireAt) {
            this.sessionId = sessionId;
            this.expireAt = expireAt;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = expireAt - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.expireAt, ((DelayItem) o).expireAt);
        }
    }

}
