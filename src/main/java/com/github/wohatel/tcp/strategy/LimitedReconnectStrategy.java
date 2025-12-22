package com.github.wohatel.tcp.strategy;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class LimitedReconnectStrategy extends ReconnectStrategy {

    private final long baseDelay;
    private final int maxAttempts;

    public LimitedReconnectStrategy(long baseDelay, int maxAttempts, Runnable success, Runnable failure) {
        this(baseDelay, maxAttempts);
        this.success = success;
        this.failure = failure;
    }

    @Override
    public boolean shouldReconnect() {
        return maxAttempts < super.attempts.get();
    }

    @Override
    public long nextDelayMillis() {
        return baseDelay;
    }
}