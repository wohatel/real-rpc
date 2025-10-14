package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.util.OneTimeLock;
import com.github.wohatel.util.VirtualThreadPool;

/**
 * The file receiver handles the event interface
 *
 * @author yaochuang 2025/03/28 09:44
 */
public class RpcFileReceiverHandlerExecProxy {

    /**
     * Remotely receive the progress of the file, which is triggered every time there is a change
     * <p>
     * Note: If the logic is processed for a long time, it is recommended to operate asynchronously
     *
     */
    public static void onProcess(RpcFileReceiverHandler rpcFileReceiverHandler, RpcFileReceiveWrapper rpcFileWrapper, long receiveSize) {
        VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onProcess(rpcFileWrapper, receiveSize));
    }

    /**
     * File Receiving Exception Execution
     */
    public static void onFailure(RpcFileReceiverHandler rpcFileReceiverHandler, final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FAIL + rpcFileWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onFailure(rpcFileWrapper, e)));
    }

    /**
     * The file is transferred finished
     *
     */
    public static void onSuccess(RpcFileReceiverHandler rpcFileReceiverHandler, final RpcFileReceiveWrapper rpcFileWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcFileWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onSuccess(rpcFileWrapper)));
    }
}