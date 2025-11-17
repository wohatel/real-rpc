package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents the input parameters for the RPC file sender functionality.
 * This class is designed using the Builder pattern for easy object construction.
 */
@Builder
@Getter
public class RpcFileSenderInput {
    /**
     * The RPC session to be used for file transfer.
     * Default value is a new RpcSession with constant value K_TEN_EIGHT.
     */
    @Builder.Default
    private RpcSession rpcSession = new RpcSession(RpcNumberConstant.K_TEN_EIGHT);
    /**
     * The context containing session-related information and configurations.
     */
    private RpcSessionContext context;
    /**
     * Configuration parameters for the file transfer operation.
     */
    private RpcFileTransConfig rpcFileTransConfig;
    /**
     * Listener interface for receiving callbacks during file sending progress.
     */
    private RpcFileSenderListener rpcFileSenderListener;
}
