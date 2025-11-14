package com.github.wohatel.interaction.common;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcFileInterrupter {

    @Getter
    private final String sessionId;

    public RpcFileInterrupter(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 中止session
     */
    public void forceInterruptSession() {
        RpcSessionTransManger.release(sessionId);
    }
}
