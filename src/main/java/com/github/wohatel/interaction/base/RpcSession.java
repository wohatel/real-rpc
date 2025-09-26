package com.github.wohatel.interaction.base;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.github.wohatel.interaction.file.RpcFileRequest;
import lombok.Getter;

/**
 * description
 *
 * @author yaochuang 2025/04/18 14:59
 */
@Getter
public class RpcSession {
    private final String sessionId;
    private final long timeOutMillis;

    public RpcSession(long timeOutMillis) {
        this(System.currentTimeMillis() + NanoIdUtils.randomNanoId(), timeOutMillis);
    }

    public RpcSession(String sessionId, long timeOutMillis) {
        this.sessionId = sessionId;
        this.timeOutMillis = timeOutMillis;
    }

    public RpcResponse toResponse() {
        return RpcResponse.fromSession(this);
    }

    public RpcSessionRequest toRpcSessionRequest() {
        return new RpcSessionRequest(this);
    }

    public RpcFileRequest toRpcFileRequest() {
        return new RpcFileRequest(this);
    }

}
