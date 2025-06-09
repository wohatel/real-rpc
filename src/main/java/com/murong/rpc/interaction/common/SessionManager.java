package com.murong.rpc.interaction.common;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * description
 *
 * @author yaochuang 2025/04/22 18:57
 */
@Data
@Log
public class SessionManager<T> {
    private final Long sessionTime;
    private volatile boolean stop;
    private final Map<String, T> container = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> timeFlushMap = new ConcurrentHashMap<>();
    private final DelayQueue<DelayItem> delayQueue = new DelayQueue<>();
    // 清理线程
    private final Thread cleanerThread;
    private final Consumer<T> sessionClose;
    /**
     * 刷新因子(若为0.4)
     * 假如一个请求对应的超时时间为10s, 如果距离超时> 4s,则不刷新,小于4s就去刷新下;
     * 也就是说: 这个值越大,刷新的越频繁
     * 如果刷新因子为负,则立即刷新
     */
    private final double flushSeed;

    public SessionManager(long sessionTime, Consumer<T> sessionClose) {
        this(sessionTime, sessionClose, 0.5);
    }

    public SessionManager(long sessionTime, Consumer<T> sessionClose, double flushSeed) {
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
        this.initSession(sessionId, resource, System.currentTimeMillis() + sessionTime);
    }

    /**
     * 初始化资源
     *
     * @param sessionId sessionId
     * @param resource  资源
     */
    public void initSession(String sessionId, T resource, Long expiredAt) {
        if (!stop) {
            T old = container.putIfAbsent(sessionId, resource);
            if (old == null) {
                timeFlushMap.put(sessionId, new AtomicLong(expiredAt));
                delayQueue.add(new DelayItem(sessionId, expiredAt));
            } else {
                throw new RuntimeException("资源已存在");
            }
        }
    }

    @SneakyThrows
    public T release(String sessionId) {
        timeFlushMap.remove(sessionId);
        return container.remove(sessionId);
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
                this.release(sessionId);
            } catch (Exception e) {
                log.log(Level.WARNING, "异常", e);
            }
        }
    }

    /**
     * 刷新时间
     *
     * @param sessionId sessionId
     * @return 刷新下sesssion的最近交互时间
     */
    public boolean flushTime(String sessionId, long sessionTime) {
        if (!stop) {
            if (container.containsKey(sessionId)) {
                AtomicLong atomicLong = timeFlushMap.get(sessionId);
                boolean needFlushForExpired = RpcSessionFlushStrategy.isNeedFlushForExpired(atomicLong.get(), sessionTime, this.flushSeed);
                if (needFlushForExpired) {
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
                AtomicLong expireAt = timeFlushMap.get(item.sessionId);
                long now = System.currentTimeMillis();
                if (expireAt == null || now >= expireAt.get()) {
                    T resource = this.release(item.sessionId);
                    if (resource != null && sessionClose != null) {
                        // 如果使用线程池关闭任务
                        VirtualThreadPool.execute(() -> {
                            try {
                                sessionClose.accept(resource);
                            } catch (Exception e) {
                                log.log(Level.WARNING, "异常", e);
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
