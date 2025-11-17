package com.github.wohatel.interaction.proxy;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.util.DefaultVirtualThreadPool;
import com.github.wohatel.util.OneTimeLock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


/**
 * A proxy class for executing RPC file request message handlers.
 * This class provides static methods to handle file transfer events in a thread-safe manner.
 * It uses a private constructor to ensure the class cannot be instantiated.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcFileRequestMsgHandlerExecProxy {

    /**
     * Remotely receive the progress of the file, which is triggered every time there is a change
     * <p>
     * Note: If the logic is processed for a long time, it is recommended to operate asynchronously
     *
     * @param rpcFileRequestMsgHandler The handler for processing file request messages
     * @param rpcFileWrapper           Wrapper containing file transfer information
     * @param receivedSize             The size of data received so far
     * @param interrupter              Object to interrupt the file transfer if needed
     */
    public static void onProcess(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, RpcFileReceiveWrapper rpcFileWrapper, long receivedSize, RpcFileInterrupter interrupter) {
        DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onProcess(rpcFileWrapper, receivedSize, interrupter));
    }

    /**
     * File Receiving Exception Execution
     * Ensures that failure handling is executed only once per session using OneTimeLock
     *
     * @param rpcFileRequestMsgHandler The handler for processing file request messages
     * @param rpcFileWrapper           Wrapper containing file transfer information
     * @param e                        Exception that occurred during file transfer
     */
    public static void onFailure(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FAIL + rpcFileWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onFailure(rpcFileWrapper, e)));
    }

    /**
     * The file is transferred finished
     * Ensures that success handling is executed only once per session using OneTimeLock
     *
     * @param rpcFileRequestMsgHandler The handler for processing file request messages
     * @param rpcFileWrapper           Wrapper containing file transfer information
     */
    public static void onSuccess(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, final RpcFileReceiveWrapper rpcFileWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcFileWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onSuccess(rpcFileWrapper)));
    }

    /**
     * The file is transferred finished
     * Ensures that finally handling is executed only once per session using OneTimeLock
     *
     * @param rpcFileRequestMsgHandler The handler for processing file request messages
     * @param rpcFileWrapper           Wrapper containing file transfer information
     */
    public static void onFinally(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, final RpcFileReceiveWrapper rpcFileWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FINALLY + rpcFileWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onFinally(rpcFileWrapper)));
    }
}