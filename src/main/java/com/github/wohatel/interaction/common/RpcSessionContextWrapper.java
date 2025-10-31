package com.github.wohatel.interaction.common;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.util.VirtualThreadPool;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author yaochuang
 */
@Getter
@AllArgsConstructor
public class RpcSessionContextWrapper {
    protected final RpcSession rpcSession;
    protected final RpcSessionContext rpcSessionContext;

    /**
     * stop receive request session msg or file
     */
    public void forceInterruptSession() {
        VirtualThreadPool.execute(() -> RpcSessionTransManger.release(this.rpcSession.getSessionId()));
    }
}
