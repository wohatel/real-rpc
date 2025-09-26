package com.github.wohatel.interaction.base;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
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
