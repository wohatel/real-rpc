package com.github.wohatel.constant;

import lombok.Getter;

/**
 *
 * @author yaochuang 2025/08/28 09:41
 */
@Getter
public enum RpcErrorEnum {

    RUNTIME(500, "server exception"),

    CONNECT(100, "connection exception"),

    SEND_MSG(300, "send msg exception"),

    HANDLE_MSG(400, "receive msg exception"),
    ;

    final int code;

    String msg;

    RpcErrorEnum(int code) {
        this(code, null);
    }

    RpcErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
