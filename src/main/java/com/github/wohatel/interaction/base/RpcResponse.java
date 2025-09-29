package com.github.wohatel.interaction.base;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcResponse extends RpcRelay {
    private String responseId;
    private String origRequestId;
    private boolean success = true;
    private String msg;
    private int code;

    public static RpcResponse fromRequest(RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            throw new IllegalArgumentException("RpcRequest is null");
        }
        if (rpcRequest instanceof RpcSessionRequest sesssionRequest) {
            RpcResponse response = new RpcResponse();
            response.setResponseId(sesssionRequest.getRpcSession().getSessionId());
            response.setOrigRequestId(sesssionRequest.getRequestId());
            return response;
        }
        RpcResponse response = new RpcResponse();
        response.setResponseId(rpcRequest.getRequestId());
        response.setOrigRequestId(rpcRequest.getRequestId());
        return response;
    }

    public static RpcResponse fromSession(RpcSession rpcSession) {
        RpcResponse response = new RpcResponse();
        response.setResponseId(rpcSession.getSessionId());
        response.setOrigRequestId(rpcSession.getSessionId());
        return response;
    }
}
