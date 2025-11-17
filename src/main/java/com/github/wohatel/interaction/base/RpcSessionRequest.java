package com.github.wohatel.interaction.base;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a request for RPC session management, extending the basic RpcRequest functionality.
 * This class is equipped with Lombok annotations for automatic generation of getters, setters,
 * toString method, and support for method chaining.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Accessors(chain = true)
public class RpcSessionRequest extends RpcRequest {
    /**
     * The RPC session associated with this request
     */
    private RpcSession rpcSession;
    /**
     * The process to be executed within the RPC session
     */
    private RpcSessionProcess sessionProcess;

    /**
     * Constructor that initializes the request with an RPC session
     *
     * @param rpcSession The RPC session to be associated with this request
     */
    public RpcSessionRequest(RpcSession rpcSession) {
        this.rpcSession = rpcSession;
    }

    /**
     * Constructor that initializes the request with an RPC session and request body
     *
     * @param rpcSession The RPC session to be associated with this request
     * @param body       The content/body of the request
     */

    public RpcSessionRequest(RpcSession rpcSession, String body) {
        this.rpcSession = rpcSession;
        this.setBody(body);
    }
}
