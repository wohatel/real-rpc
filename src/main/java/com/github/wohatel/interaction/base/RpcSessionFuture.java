package com.github.wohatel.interaction.base;


import lombok.Data;
import lombok.experimental.Accessors;

/** * @author yaochuang
 */
@Accessors(chain = true)
@Data
public class RpcSessionFuture extends RpcFuture {

    private volatile RpcSessionProcess rpcSessionProcess;

    private String channelId;

    public boolean isSessionFinish() {
        return rpcSessionProcess == RpcSessionProcess.FiNISH;
    }

    public boolean isSessionRunning() {
        return rpcSessionProcess == RpcSessionProcess.ING;
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
