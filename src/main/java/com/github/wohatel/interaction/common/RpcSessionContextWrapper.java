package com.github.wohatel.interaction.common;

import com.github.wohatel.interaction.base.RpcSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This is a wrapper class for RPC session context.
 * It provides a convenient way to handle both RPC session and its context together.
 *
 * @author yaochuang
 */
@Getter // Lombok annotation to generate getter methods for all fields
@AllArgsConstructor // Lombok annotation to generate a constructor with all arguments
public class RpcSessionContextWrapper {
    /**
     * The RPC session object that holds session-related information
     */
    protected final RpcSession rpcSession;
    /**
     * The RPC session context that contains context-specific data
     */
    protected final RpcSessionContext rpcSessionContext;
}
