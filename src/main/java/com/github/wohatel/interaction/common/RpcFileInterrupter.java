package com.github.wohatel.interaction.common;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * RpcFileInterrupter class for handling file transfer interruptions in RPC sessions.
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcFileInterrupter {

    /**
     * The session ID associated with this file interrupter.
     * Used to identify which session to interrupt.
     */
    @Getter
    private final String sessionId;

    /**
     * Constructor for RpcFileInterrupter.
     *
     * @param sessionId The unique identifier for the RPC session
     */
    public RpcFileInterrupter(String sessionId) {
        this.sessionId = sessionId;
    }


    /**
     * Forcefully interrupts and releases a session by its ID
     * This method calls the RpcSessionTransManger to release the specified session
     */
    public void forceInterruptSession() {
        // Call the release method of RpcSessionTransManger to terminate the session with the given sessionId
        RpcSessionTransManger.release(sessionId);
    }
}
