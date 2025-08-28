package com.murong.rpc.interaction.file;

import com.murong.rpc.constant.RpcSysEnum;
import com.murong.rpc.interaction.common.VirtualThreadPool;
import com.murong.rpc.util.OneTimeLock;
import lombok.RequiredArgsConstructor;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
@RequiredArgsConstructor
public class RpcFileSenderListenerProxy {

    private final RpcFileSenderListener rpcFileSenderListener;

    /**
     * 传输成功触
     */
    public void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {
        if (rpcFileSenderListener != null) {
            OneTimeLock.runOnce(RpcSysEnum.SENDER.name() + RpcSysEnum.SUCCESS + rpcFileSenderWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileSenderListener.onSuccess(rpcFileSenderWrapper)));
        }
    }

    /**
     * 传输失败后触发
     */
    public void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {
        if (rpcFileSenderListener != null) {
            OneTimeLock.runOnce(RpcSysEnum.SENDER.name() + RpcSysEnum.FAIL + rpcFileSenderWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileSenderListener.onFailure(rpcFileSenderWrapper, errorMsg)));
        }
    }

    /**
     * 监控传输进程
     */
    public void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {
        if (rpcFileSenderListener != null) {
            VirtualThreadPool.execute(() -> rpcFileSenderListener.onProcess(rpcFileSenderWrapper, process));
        }
    }

}
