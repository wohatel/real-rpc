package com.github.wohatel.interaction.common;

import com.github.wohatel.interaction.base.RpcSession;
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
}
