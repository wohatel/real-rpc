package com.github.wohatel.interaction.proxy;

import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.util.DefaultVirtualThreadPool;
import com.github.wohatel.util.GoalKeeper;
import lombok.Getter;


/**
 * A proxy class for executing RPC file request message handlers.
 * This class provides static methods to handle file transfer events in a thread-safe manner.
 * It uses a private constructor to ensure the class cannot be instantiated.
 */
public class RpcFileRequestMsgHandlerExecProxy {

    @Getter
    private RpcFileRequestMsgHandler rpcFileRequestMsgHandler;
    private final GoalKeeper goalKeeper;

    public RpcFileRequestMsgHandlerExecProxy(RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        this.rpcFileRequestMsgHandler = rpcFileRequestMsgHandler;
        this.goalKeeper = new GoalKeeper();
    }

    /**
     * Remotely receive the progress of the file, which is triggered every time there is a change
     * <p>
     * Note: If the logic is processed for a long time, it is recommended to operate asynchronously
     *
     * @param rpcFileWrapper           Wrapper containing file transfer information
     * @param receivedSize             The size of data received so far
     * @param interrupter              Object to interrupt the file transfer if needed
     */
    public void onProcess(RpcFileReceiveWrapper rpcFileWrapper, long receivedSize, RpcFileInterrupter interrupter) {
        DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onProcess(rpcFileWrapper, receivedSize, interrupter));
    }

    /**
     * The file is transferred finished
     * Ensures that success handling is executed only once per session using OneTimeLock
     *
     * @param rpcFileWrapper Wrapper containing file transfer information
     */
    public void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {
        if (goalKeeper.once("onSuccess")) {
            DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onSuccess(rpcFileWrapper));
        }
    }

    /**
     * File Receiving Exception Execution
     * Ensures that failure handling is executed only once per session using OneTimeLock
     *
     * @param rpcFileWrapper           Wrapper containing file transfer information
     * @param e                        Exception that occurred during file transfer
     */
    public void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
        if (goalKeeper.once("onFailure")) {
            DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onFailure(rpcFileWrapper, e));
        }
    }

    /**
     * The file is transferred finished
     * Ensures that finally handling is executed only once per session using OneTimeLock
     *
     * @param rpcFileWrapper           Wrapper containing file transfer information
     */
    public void onFinally(final RpcFileReceiveWrapper rpcFileWrapper) {
        if (goalKeeper.once("onFinally")) {
            DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onFinally(rpcFileWrapper));
        }
    }
}