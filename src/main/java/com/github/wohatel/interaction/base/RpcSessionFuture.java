package com.github.wohatel.interaction.base;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author yaochuang
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class RpcSessionFuture extends RpcFuture {

    @Getter
    @Setter
    private volatile RpcSessionProcess rpcSessionProcess;

    @Getter
    @Setter
    private String uniqueId;

    public RpcSessionFuture(long timeOut) {
        super(timeOut);
    }

}
