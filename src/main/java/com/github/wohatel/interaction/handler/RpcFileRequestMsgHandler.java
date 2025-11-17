package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileSignature;


/**
 * Interface for handling RPC file request messages.
 * Defines methods for file transfer operations including determining storage location,
 * handling progress, and managing exceptions.
 */
public interface RpcFileRequestMsgHandler {

    /**
     * Determine the file storage location (called before the file transfer starts)
     * @param rpcSession The RPC session associated with the file transfer
     * @param context The context of the RPC session containing additional information
     * @param fileInfo Information about the file to be transferred
     * @return RpcFileSignature containing the determined storage location details
     */
    RpcFileSignature getTargetFile(final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo);

    /**
     * Remotely receive the progress of the file, which is triggered every time there is a change
     * @param rpcFileWrapper Wrapper containing file information and transfer details
     * @param receivedSize The size of data that has been received so far
     * @param interrupter Object that can be used to interrupt the file transfer if needed
     */
    default void onProcess(final RpcFileReceiveWrapper rpcFileWrapper, long receivedSize,final RpcFileInterrupter interrupter) {

    }


    /**
     * Default implementation of the onFailure method for handling file reception failures.
     * This method is called when an error occurs during the file receiving process.
     *
     * @param rpcFileWrapper The wrapper containing information about the file reception attempt
     * @param e              The exception that occurred during the file reception process
     */
    default void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {

    }

    /**
     * Default method called when a file receive operation succeeds.
     * This is a callback method that handles the successful reception of a file through RPC.
     *
     * @param rpcFileWrapper The wrapper object containing the received file information
     *                       and metadata from the RPC operation.
     */
    default void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {

        // This is a default empty implementation that can be overridden
        // by implementing classes to handle successful file reception.
        // The method receives a RpcFileReceiveWrapper object which contains
        // all necessary information about the received file.
    }


    /**
     * Default implementation of the onFinally method for handling the final stage of file reception.
     * This method is part of the file receiving process and is called after the file reception is completed,
     * regardless of whether it was successful or not.
     *
     * @param rpcFileWrapper The wrapper object containing information about the received file,
     *                       including file metadata, status, and any relevant context.
     *                       It is marked as final to ensure it is not modified within the method.
     */
    default void onFinally(final RpcFileReceiveWrapper rpcFileWrapper) {

    }
}