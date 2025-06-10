package com.murong.rpc.interaction.base;

import lombok.Getter;

import java.util.UUID;

/**
 * description
 *
 * @author yaochuang 2025/04/18 14:59
 */
@Getter
public class RpcSession {
    private String sessionId;
    private long timeOutMillis;
    private String topic;
    private String[] metters;

    public RpcSession(long timeOutMillis) {
        this(timeOutMillis, null);
    }

    private RpcSession(String sessionId, long timeOutMillis, String topic, String... metters) {
        this.sessionId = sessionId;
        this.timeOutMillis = timeOutMillis;
        this.topic = topic;
        this.metters = metters;
    }

    public RpcSession(long timeOutMillis, String topic, String... metters) {
        this(UUID.randomUUID().toString(), timeOutMillis, topic, metters);
    }

    public RpcResponse toResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(this.sessionId);
        return response;
    }

}
