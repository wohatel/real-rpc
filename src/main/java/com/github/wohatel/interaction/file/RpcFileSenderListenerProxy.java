package com.github.wohatel.interaction.file;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.interaction.common.VirtualThreadPool;
import com.github.wohatel.util.OneTimeLock;
import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static com.github.wohatel.util.ReflectUtil.isOverridingInterfaceDefaultMethod;
import static com.github.wohatel.util.ReflectUtil.isOverridingInterfaceDefaultMethodByImplObj;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
public class RpcFileSenderListenerProxy {

    private final RpcFileSenderListener rpcFileSenderListener;
    private final boolean isProcessOverride;

    public RpcFileSenderListenerProxy(RpcFileSenderListener rpcFileSenderListener) {
        this.rpcFileSenderListener = rpcFileSenderListener;
        if (rpcFileSenderListener != null) {
            isProcessOverride = isOverridingInterfaceDefaultMethodByImplObj(rpcFileSenderListener, "onProcess");
        } else {
            isProcessOverride = false;
        }
    }

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
            VirtualThreadPool.execute(isProcessOverride, () -> rpcFileSenderListener.onProcess(rpcFileSenderWrapper, process));
        }
    }

}
