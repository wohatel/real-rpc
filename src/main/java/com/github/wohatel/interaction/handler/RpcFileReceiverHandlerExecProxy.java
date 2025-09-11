package com.github.wohatel.interaction.handler;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.common.VirtualThreadPool;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.util.OneTimeLock;

/**
 * 文件接收端处理事件接口
 *
 * @author yaochuang 2025/03/28 09:44
 */
public class RpcFileReceiverHandlerExecProxy {

    /**
     * 远程接收文件的进度,每次有变化就会触发
     * <p>
     * 注意: 如果处理逻辑比较久,建议异步操作
     *
     */
    public static void onProcess(RpcFileReceiverHandler rpcFileReceiverHandler, RpcFileReceiveWrapper rpcFileWrapper, long recieveSize) {
        VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onProcess(rpcFileWrapper, recieveSize));
    }

    /**
     * 文件接收异常执行
     *
     * @param e 发生的异常
     */
    public static void onFailure(RpcFileReceiverHandler rpcFileReceiverHandler, final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.FAIL + rpcFileWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onFailure(rpcFileWrapper, e)));
    }

    /**
     * 文件整体传输完毕
     *
     */
    public static void onSuccess(RpcFileReceiverHandler rpcFileReceiverHandler, final RpcFileReceiveWrapper rpcFileWrapper) {
        OneTimeLock.runOnce(RpcSysEnum.RECEIVER.name() + RpcSysEnum.SUCCESS + rpcFileWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onSuccess(rpcFileWrapper)));
    }
}