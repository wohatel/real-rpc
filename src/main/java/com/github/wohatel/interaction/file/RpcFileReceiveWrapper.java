package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import lombok.Getter;

import java.io.File;

/**
 * Wrapper class for handling RPC file reception operations.
 * Extends RpcSessionContextWrapper to provide file transfer specific functionality.
 */
@Getter
public class RpcFileReceiveWrapper extends RpcSessionContextWrapper {

    /**
     * Constructor for RpcFileReceiveWrapper.
     * Initializes the wrapper with necessary parameters for file reception.
     *
     * @param rpcSession      The RPC session for the file transfer
     * @param context         The session context containing additional information
     * @param file            The file to be received
     * @param transModel      The model for file transfer operations
     * @param rpcFileInfo     Information about the RPC file being transferred
     * @param needTransLength The total length of data that needs to be transferred
     */
    public RpcFileReceiveWrapper(RpcSession rpcSession, RpcSessionContext context, File file, RpcFileTransModel transModel, RpcFileInfo rpcFileInfo, long needTransLength) {
        super(rpcSession, context);
        this.file = file;
        this.transModel = transModel;
        this.rpcFileInfo = rpcFileInfo;
        this.needTransLength = needTransLength;
    }

    /**
     * The file to be received
     */
    private final File file;

    /**
     * The model for file transfer operations
     */
    private final RpcFileTransModel transModel;

    /**
     * Information about the RPC file being transferred
     */
    private final RpcFileInfo rpcFileInfo;

    /** The total length of data that needs to be transferred */
    private final long needTransLength;
}
