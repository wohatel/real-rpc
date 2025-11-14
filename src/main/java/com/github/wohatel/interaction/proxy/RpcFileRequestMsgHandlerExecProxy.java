package com.github.wohatel.interaction.proxy;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.util.OneTimeLock;
import com.github.wohatel.util.DefaultVirtualThreadPool;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The file receiver handles the event interface
 *
 * @author yaochuang 2025/03/28 09:44
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcFileRequestMsgHandlerExecProxy {

    /**
     * Remotely receive the progress of the file, which is triggered every time there is a change
     * <p>
     * Note: If the logic is processed for a long time, it is recommended to operate asynchronously
     *
     */
    public static void onProcess(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, RpcFileReceiveWrapper rpcFileWrapper, long receivedSize, RpcFileInterrupter interrupter) {
        DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onProcess(rpcFileWrapper, receivedSize, interrupter));
    }

    /**
     * File Receiving Exception Execution
     */
    public static void onFailure(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FAIL + rpcFileWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onFailure(rpcFileWrapper, e)));
    }

    /**
     * The file is transferred finished
     *
     */
    public static void onSuccess(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, final RpcFileReceiveWrapper rpcFileWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcFileWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onSuccess(rpcFileWrapper)));
    }

    /**
     * The file is transferred finished
     *
     */
    public static void onFinally(RpcFileRequestMsgHandler rpcFileRequestMsgHandler, final RpcFileReceiveWrapper rpcFileWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FINALLY + rpcFileWrapper.getRpcSession().getSessionId(), () -> DefaultVirtualThreadPool.execute(() -> rpcFileRequestMsgHandler.onFinally(rpcFileWrapper)));
    }
}