package com.murong.rpc.interaction.base;

import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileRequest;
import lombok.Getter;

import java.util.UUID;

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
        this(UUID.randomUUID().toString(), timeOutMillis);
    }

    public RpcSession(String sessionId, long timeOutMillis) {
        this.sessionId = sessionId;
        this.timeOutMillis = timeOutMillis;
    }


    public RpcResponse toResponse() {
        RpcResponse response = new RpcResponse();
        response.setRequestId(this.sessionId);
        return response;
    }

    public RpcSessionRequest toRpcSessionRequest() {
        return new RpcSessionRequest(this);
    }

    public RpcFileRequest toRpcFileRequest() {
        return new RpcFileRequest(this);
    }

}
