package com.github.wohatel.interaction.file;

import com.github.wohatel.constant.RpcSysEnum;
import com.github.wohatel.util.OneTimeLock;
import com.github.wohatel.util.VirtualThreadPool;

import static com.github.wohatel.util.ReflectUtil.isOverridingInterfaceDefaultMethodByImplObj;


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

    public void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {
        if (rpcFileSenderListener != null) {
            OneTimeLock.runOnce(RpcSysEnum.SENDER.name() + RpcSysEnum.SUCCESS + rpcFileSenderWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileSenderListener.onSuccess(rpcFileSenderWrapper)));
        }
    }


    public void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {
        if (rpcFileSenderListener != null) {
            OneTimeLock.runOnce(RpcSysEnum.SENDER.name() + RpcSysEnum.FAIL + rpcFileSenderWrapper.getRpcSession().getSessionId(), () -> VirtualThreadPool.execute(() -> rpcFileSenderListener.onFailure(rpcFileSenderWrapper, errorMsg)));
        }
    }


    public void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {
        if (rpcFileSenderListener != null) {
            VirtualThreadPool.execute(isProcessOverride, () -> rpcFileSenderListener.onProcess(rpcFileSenderWrapper, process));
        }
    }

}
