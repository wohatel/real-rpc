package com.github.wohatel.interaction.base;

import com.github.wohatel.util.RandomUtil;
import lombok.Getter;

/**
 * Represents a session for RPC (Remote Procedure Call) communication.
 * Contains session ID and timeout information.
 *
 * @author yaochuang 2025/04/18 14:59
 */
@Getter
public class RpcSession {
    // Unique identifier for the RPC session
    private final String sessionId;
    // Session timeout duration in milliseconds
    private final long timeOutMillis;

    /**
     * Constructs a new RpcSession with a randomly generated session ID and specified timeout.
     *
     * @param timeOutMillis the duration in milliseconds after which the session will timeout
     */
    public RpcSession(long timeOutMillis) {
        this(RandomUtil.randomUUIDWithTime(), timeOutMillis);
    }

    /**
     * Constructs a new RpcSession with specified session ID and timeout.
     *
     * @param sessionId     the unique identifier for the session
     * @param timeOutMillis the duration in milliseconds after which the session will timeout
     */
    public RpcSession(String sessionId, long timeOutMillis) {
        this.sessionId = sessionId;
        this.timeOutMillis = timeOutMillis;
    }

    /**
     * Converts this session to an RpcReaction object.
     *
     * @return RpcReaction object representing this session
     */
    public RpcReaction toReaction() {
        return RpcReaction.fromSession(this);
    }
}
