package com.murong.rpc.constant;

import lombok.Getter;

/**
 * description
 *
 * @author yaochuang 2025/08/28 09:41
 */
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

    public RpcException(String message, Throwable cause) {
        super(message, cause);
        this.code = RpcErrorEnum.RUNTIME.getCode();
    }
}
