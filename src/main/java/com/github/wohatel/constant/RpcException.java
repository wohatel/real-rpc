package com.github.wohatel.constant;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Custom exception class for RPC (Remote Procedure Call) errors.
 * This class extends RuntimeException to provide unchecked exception handling for RPC operations.
 *
 * @author yaochuang 2025/08/28 09:41
 */
@Slf4j  // Lombok annotation for adding a logger to the class
@Getter  // Lombok annotation to automatically generate getter methods for all fields
public class RpcException extends RuntimeException {

    private int code;  // Error code associated with the RPC exception

    /**
     * Constructor for creating an RPC exception with a custom message.
     *
     * @param message The detailed error message
     */
    public RpcException(String message) {
        super(message);
    }

    /**
     * Constructor for creating an RPC exception using an RpcErrorEnum.
     *
     * @param rpcError The RpcErrorEnum containing error code and message
     */
    public RpcException(RpcErrorEnum rpcError) {
        this(rpcError.msg);
        this.code = rpcError.getCode();
    }

    /**
     * Constructor for creating an RPC exception with both custom message and RpcErrorEnum.
     *
     * @param rpcError The RpcErrorEnum containing error code
     * @param message  The custom detailed error message
     */
    public RpcException(RpcErrorEnum rpcError, String message) {
        this(message);
        this.code = rpcError.getCode();
    }
}
