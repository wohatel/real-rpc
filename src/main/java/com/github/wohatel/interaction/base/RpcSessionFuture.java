package com.github.wohatel.interaction.base;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author yaochuang
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
public class RpcSessionFuture extends RpcFuture {

    private volatile RpcSessionProcess rpcSessionProcess;

    private String uniqueId;

    public boolean isSessionFinish() {
        return rpcSessionProcess == RpcSessionProcess.FiNISHED;
    }

    public boolean isSessionRunning() {
        return rpcSessionProcess == RpcSessionProcess.RUNNING;
    }

    public boolean isSessionFailed() {
        return rpcSessionProcess == null;
    }

    public RpcSessionFuture() {
    }

    public RpcSessionFuture(long timeOut) {
        this.setTimeOut(timeOut);
    }

}
