package com.github.wohatel.tcp.strategy;

import com.github.wohatel.util.RunnerUtil;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class ReconnectStrategy {

    protected final AtomicInteger attempts = new AtomicInteger(0);

    @Setter
    protected Runnable success;

    @Setter
    protected Runnable failure;

    public abstract boolean shouldReconnect();

    /**
     * 下一次重连延迟（毫秒）
     */
    public abstract long nextDelayMillis();

    /**
     * 重连成功
     * This method is called when the reconnection is successful.
     * It indicates that the connection has been re-established and the system can resume normal operations.
     */
    public void onSuccess() {
        attempts.setRelease(0);
        if (success != null) {
            RunnerUtil.execSilent(() -> success.run());
        }
    }

    /**
     * 重连失败
     */
    public void onFailure() {
        attempts.incrementAndGet();
        if (failure != null) {
            RunnerUtil.execSilent(() -> failure.run());
        }
    }
}