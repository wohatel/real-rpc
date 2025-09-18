package com.github.wohatel.interaction.base;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RpcSessionRequest extends RpcRequest {
    private RpcSession rpcSession;
    private RpcSessionProcess sessionProcess;

    public RpcSessionRequest(RpcSession rpcSession) {
        this.rpcSession = rpcSession;
    }

    public RpcSessionRequest(RpcSession rpcSession, String body) {
        this.rpcSession = rpcSession;
        this.setBody(body);
    }

    @Override
    public RpcResponse toResponse() {
        RpcResponse response = new RpcResponse();
        response.setResponseId(this.rpcSession.getSessionId());
        response.setOrigRequestId(this.getRequestId());
        return response;
    }

    public boolean isSessionRequest() {
        return this.sessionProcess != null && sessionProcess == RpcSessionProcess.ING;
    }

    public boolean isSessionStart() {
        return this.sessionProcess != null && sessionProcess == RpcSessionProcess.START;
    }

    public boolean isSessionFinish() {
        return this.sessionProcess != null && sessionProcess == RpcSessionProcess.FiNISH;
    }

}
