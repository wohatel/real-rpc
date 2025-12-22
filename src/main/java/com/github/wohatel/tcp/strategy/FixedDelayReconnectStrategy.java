package com.github.wohatel.tcp.strategy;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FixedDelayReconnectStrategy extends ReconnectStrategy {

    private final long baseDelay;

    public FixedDelayReconnectStrategy(long baseDelay, Runnable success, Runnable failure) {
        this(baseDelay);
        this.success = success;
        this.failure = failure;
    }


    @Override
    public boolean shouldReconnect() {
        return true;
    }

    @Override
    public long nextDelayMillis() {
        return baseDelay;
    }
}