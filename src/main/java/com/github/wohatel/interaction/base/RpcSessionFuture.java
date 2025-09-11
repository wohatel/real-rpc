package com.github.wohatel.interaction.base;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yaochuang
 */
@Accessors(chain = true)
@Data
public class RpcSessionFuture extends RpcFuture {

    private volatile boolean isSessionFinish;

    public RpcSessionFuture() {
    }

    public RpcSessionFuture(long timeOut) {
        this.setTimeOut(timeOut);
    }

}
