package com.murong.rpc.constant;

import lombok.Getter;

/**
 * description
 *
 * @author yaochuang 2025/08/28 09:41
 */
@Getter
public enum RpcErrorEnum {

    RUNTIME(500, "服务器异常"),

    CONNECT(100, "连接异常"),

    SEND_MSG(200, "数据发送异常"),

    HANDLE_MSG(300, "数据接收处理异常");

    final int code;

    String msg;

    RpcErrorEnum(int code) {
        this.code = code;
    }

    RpcErrorEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
