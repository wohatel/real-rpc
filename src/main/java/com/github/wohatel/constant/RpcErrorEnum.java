package com.github.wohatel.constant;

import lombok.Getter;

/**
 * Enumeration class for RPC errors
 * Provides standardized error codes and messages for various RPC exception scenarios
 *
 * @author yaochuang 2025/08/28 09:41
 */
@Getter
public enum RpcErrorEnum {

    // Runtime related errors
    RUNTIME(500, "server exception"),

    // Connection related errors
    CONNECT(100, "connection exception"),

    // Message sending errors
    SEND_MSG(300, "send msg exception"),

    // Message handling errors
    HANDLE_MSG(400, "receive msg exception"),

    // Session management errors
    SESSION_LOSE(9, "session is lose"),
    ;

    // Error code field
    final int code;

    // Error message field
    final String msg;

    /**
     * Constructor for RpcErrorEnum
     *
     * @param code Error code
     * @param msg  Error message
     */
    RpcErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
