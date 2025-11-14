package com.github.wohatel.interaction.base;

import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.util.RandomUtil;
import lombok.Getter;

/**
 *
 * @author yaochuang 2025/04/18 14:59
 */
@Getter
public class RpcSession {
    private final String sessionId;
    private final long timeOutMillis;

    public RpcSession(long timeOutMillis) {
        this(RandomUtil.randomUUIDWithTime(), timeOutMillis);
    }

    public RpcSession(String sessionId, long timeOutMillis) {
        this.sessionId = sessionId;
        this.timeOutMillis = timeOutMillis;
    }

    public RpcReaction toReaction() {
        return RpcReaction.fromSession(this);
    }

    public RpcSessionRequest toRpcSessionRequest() {
        return new RpcSessionRequest(this);
    }

    public RpcFileRequest toRpcFileRequest() {
        return new RpcFileRequest(this);
    }

}
