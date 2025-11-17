package com.github.wohatel.interaction.file;


/**
 * Interface for listening to file sending events in RPC communication.
 * This interface provides default implementations for all methods,
 * allowing listeners to implement only the events they are interested in.
 */
public interface RpcFileSenderListener {

    /**
     * Called when the file is successfully sent.
     * This method is called by default and does nothing.
     * Override this method to handle successful file sending.
     *
     * @param rpcFileSenderWrapper The wrapper containing information about the file sender
     */
    default void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {

    }

    /**
     * only remote agreed and the file sending fails called
     * This method is called by default and does nothing.
     * Override this method to handle file sending failures.
     *
     * @param rpcFileSenderWrapper The wrapper containing information about the file sender
     * @param errorMsg             The error message describing why the sending failed
     */
    default void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {

    }

    /**
     * Called to report the progress of the file transfer.
     * This method is called by default and does nothing.
     * Override this method to handle progress updates.
     *
     * @param rpcFileSenderWrapper The wrapper containing information about the file sender
     * @param process              The object containing transfer progress information
     */
    default void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {
    }

}
