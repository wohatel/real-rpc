package com.murong.rpc.interaction.common;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * description
 *
 * @author yaochuang 2025/04/22 18:57
 */
@Data
@Slf4j
public class SessionManager<T> {
    private final Long sessionTime;
    private volatile boolean stop;
    private final Map<String, T> container = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timeFlushMap = new ConcurrentHashMap<>();
    private final Map<String, Object> dataMap = new ConcurrentHashMap<>();
    private final DelayQueue<DelayItem> delayQueue = new DelayQueue<>();
    private final Thread cleanerThread;
    private final BiConsumer<String, T> sessionClose;
    private BiPredicate<String, T> autoFlushPredicate;

    /**
     * 刷新因子(若为0.4)
     * 假如一个请求对应的超时时间为10s, 如果距离超时> 4s,则不刷新,小于4s就去刷新下;
     * 也就是说: 这个值越大,刷新的越频繁
     * 如果刷新因子为负,则立即刷新
     */
    private final double flushSeed;

    public SessionManager(long sessionTime, BiConsumer<String, T> sessionClose) {
        this(sessionTime, sessionClose, 0.618);
    }

    public SessionManager(long sessionTime, BiConsumer<String, T> sessionClose, double flushSeed) {
        this.flushSeed = flushSeed;
        this.sessionTime = sessionTime;
        if (sessionTime <= 0) {
            throw new RuntimeException("会话时间错误");
        }
        this.sessionClose = sessionClose;
        cleanerThread = Thread.startVirtualThread(this::cleanerLoop);
    }

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
        this.initSession(sessionId, resource, null, null);
    }

    /**
     * 初始化资源
     *
     * @param sessionId sessionId
     * @param resource  资源
     */
    public void initSession(String sessionId, T resource, Long expiredAt) {
        this.initSession(sessionId, resource, expiredAt, null);
    }

    /**
     * 初始化资源
     *
     * @param sessionId sessionId
     * @param resource  资源
     */
    public void initSession(String sessionId, T resource, Long expiredAt, Object data) {
        if (!stop) {
            T old = container.putIfAbsent(sessionId, resource);
            if (old == null) {
                long finalExpiredAt = expiredAt == null ? System.currentTimeMillis() + sessionTime : expiredAt;
                timeFlushMap.put(sessionId, new AtomicLong(finalExpiredAt));
                delayQueue.add(new DelayItem(sessionId, finalExpiredAt));
                if (data != null) {
                    dataMap.put(sessionId, data);
                }
            } else {
                throw new RuntimeException("session资源已存在");
            }
        }
    }

    public Object getData(String sessionId) {
        return dataMap.get(sessionId);
    }

    @SneakyThrows
    public T release(String sessionId) {
        timeFlushMap.remove(sessionId);
        dataMap.remove(sessionId);
        return container.remove(sessionId);
    }

    @SneakyThrows
    public void releaseAndSessionClose(String sessionId) {
        T release = release(sessionId);
        if (this.sessionClose != null) {
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
        this.setStop(true);
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
                log.error("销毁管理器异常", e);
            }
        }
    }

    /**
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下sesssion的最近交互时间
     */
    public boolean flushTime(String sessionId, long sessionTime, boolean force) {
        if (!stop) {
            if (container.containsKey(sessionId)) {
                AtomicLong atomicLong = timeFlushMap.get(sessionId);
                if (force || RpcSessionFlushStrategy.isNeedFlushForExpired(atomicLong.get(), sessionTime, this.flushSeed)) {
                    long expiredAt = System.currentTimeMillis() + sessionTime;
                    delayQueue.add(new DelayItem(sessionId, expiredAt));
                    timeFlushMap.get(sessionId).set(expiredAt);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下sesssion的最近交互时间
     */
    public boolean flushTime(String sessionId, long sessionTime) {
        return flushTime(sessionId, sessionTime, false);
    }

    /**
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下sesssion的最近交互时间
     */
    public boolean flushTime(String sessionId) {
        return this.flushTime(sessionId, sessionTime);
    }

    /**
     * 循环清理
     */
    private void cleanerLoop() {
        while (!stop) {
            try {
                DelayItem item = delayQueue.take();
                T resource = container.get(item.sessionId);
                if (resource == null) {// 说明已经被移除
                    continue;
                }
                // 自动控制测试是否需要
                if (autoTest(item.sessionId, resource)) {
                    flushTime(item.sessionId, sessionTime, true);
                    continue;
                }
                AtomicLong expireAt = timeFlushMap.get(item.sessionId);
                long now = System.currentTimeMillis();
                if (now >= expireAt.get()) {
                    // 释放资源
                    this.release(item.sessionId);
                    if (sessionClose != null) {
                        // 如果使用线程池关闭任务
                        VirtualThreadPool.execute(() -> {
                            try {
                                sessionClose.accept(item.sessionId, resource);
                            } catch (Exception e) {
                                log.error("cleanerLoop异常", e);
                            }
                        });
                    }
                } else {
                    delayQueue.offer(new DelayItem(item.sessionId, expireAt.get()));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 校验验证是否成功
     */
    private boolean autoTest(String sessionId, T resource) {
        if (this.autoFlushPredicate == null) {
            return false;
        }
        try {
            return autoFlushPredicate.test(sessionId, resource);
        } catch (Exception e) {
            log.error("autoTest校验失败", e);
        }
        return false;
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
