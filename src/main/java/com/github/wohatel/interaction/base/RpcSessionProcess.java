package com.github.wohatel.interaction.base;

import lombok.Getter;

/**
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
    FiNISHED
}
