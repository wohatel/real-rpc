package com.github.wohatel.constant;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * description
 *
 * @author yaochuang 2025/08/28 09:41
 */
@Slf4j
@Getter
public class RpcException extends RuntimeException {

    private int code;

    public RpcException(String message) {
        super(message);
    }

    public RpcException(RpcErrorEnum rpcError) {
        this(rpcError.msg);
        this.code = rpcError.getCode();
    }

    public RpcException(RpcErrorEnum rpcError, String message) {
        this(message);
        this.code = rpcError.getCode();
    }

    public static void printLineNumber(Throwable throwable) {
        printLineNumber(throwable, 2);
    }

    public static void printLineNumber(Throwable throwable, int level) {
        StackTraceElement[] stack = throwable.getStackTrace();
        // stack[0] 是当前方法 myMethod
        // stack[1] 是调用 myMethod 的地方
        int length = stack.length;
        if (level < length) {
            StackTraceElement caller = stack[level];
            log.error("Called from: " + caller.getClassName() + "." + caller.getMethodName() + "()" + " at line " + caller.getLineNumber());
        }
    }
}
