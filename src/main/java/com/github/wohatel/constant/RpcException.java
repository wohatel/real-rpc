package com.github.wohatel.constant;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
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
}
