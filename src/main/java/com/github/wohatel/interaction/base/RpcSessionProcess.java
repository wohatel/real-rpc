package com.github.wohatel.interaction.base;

import lombok.Getter;

/**
 * Enumeration representing the different states of an RPC session processing.
 * This enum defines the lifecycle states of an RPC session.
 *
 * @author yaochuang 2025/04/18 14:59
 */
@Getter
public enum RpcSessionProcess {
    /**
     * try to start session
     */
    TOSTART,

    /**
     * is running in session
     */
    RUNNING,

    /**
     * session is finished
     */
    FINISHED
}
