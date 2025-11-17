package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

/**
 * A wrapper class for handling RPC file sending operations.
 * This class encapsulates the necessary parameters and session information for file transfer.
 */
@RequiredArgsConstructor
public class RpcFileSenderWrapper {

    /**
     * The RPC session used for file transfer communication.
     * This session maintains the connection and state for the RPC operation.
     */
    @Getter
    private final RpcSession rpcSession;

    /**
     * The local file that needs to be transferred through RPC.
     * This represents the source file on the client side.
     */
    @Getter
    private final File localFile;

    /**
     * The transfer model that defines the parameters and behavior of the file transfer.
     * This includes settings like buffer size, progress tracking, etc.
     */
    @Getter
    private final RpcFileTransModel transModel;

}
