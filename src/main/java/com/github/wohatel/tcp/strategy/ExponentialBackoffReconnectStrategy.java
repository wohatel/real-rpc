package com.github.wohatel.tcp.strategy;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExponentialBackoffReconnectStrategy extends ReconnectStrategy {

    private final long baseDelay;
    private final long maxDelay;

    public ExponentialBackoffReconnectStrategy(long baseDelay, long maxDelay, Runnable success, Runnable failure) {
        this(baseDelay, maxDelay);
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean shouldReconnect() {
        return true;
    }

    @Override
    public long nextDelayMillis() {
        int attempts = super.attempts.get();
        long delay = baseDelay * (1L << Math.min(attempts, 10));
        return Math.min(delay, maxDelay);
    }
}