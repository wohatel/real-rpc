package com.github.wohatel.interaction.proxy;

import com.github.wohatel.interaction.file.RpcFileSenderListener;
import com.github.wohatel.interaction.file.RpcFileSenderWrapper;
import com.github.wohatel.interaction.file.RpcFileTransProcess;
import com.github.wohatel.util.DefaultVirtualThreadPool;

import static com.github.wohatel.util.ReflectUtil.isOverridingInterfaceDefaultMethodByImplObj;


/**
 * A proxy class for RpcFileSenderListener that handles thread execution and method override detection.
 * This proxy ensures that callbacks are executed in a controlled manner using thread pools.
 */
public class RpcFileSenderListenerProxy {

    private final int[] status;

    /**
     * The actual listener instance that will be called by this proxy.
     */
    private final RpcFileSenderListener rpcFileSenderListener;
    /**
     * Flag indicating whether the onProcess method is overridden in the implementation.
     * This is used to determine execution strategy in the onProcess method.
     */
    private final boolean isProcessOverride;

    /**
     * Constructor for RpcFileSenderListenerProxy.
     *
     * @param rpcFileSenderListener The listener instance to be proxied
     */
    public RpcFileSenderListenerProxy(RpcFileSenderListener rpcFileSenderListener) {
        this.rpcFileSenderListener = rpcFileSenderListener;
        // Check if the implementation overrides the default onProcess method
        if (rpcFileSenderListener != null) {
            isProcessOverride = isOverridingInterfaceDefaultMethodByImplObj(rpcFileSenderListener, "onProcess");
        } else {
            isProcessOverride = false;
        }
        status = new int[2];
    }

    /**
     * Handles successful file sending events.
     * Executes the callback in a virtual thread pool with a one-time lock to prevent duplicate executions.
     *
     * @param rpcFileSenderWrapper The wrapper containing file sender information
     */
    public void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {
        if (rpcFileSenderListener != null && status[0] == 0) {
            status[0] = 1;
            DefaultVirtualThreadPool.execute(() -> rpcFileSenderListener.onSuccess(rpcFileSenderWrapper));
        }
    }


    /**
     * Handles failure scenarios in the RPC file sending process.
     * This method is called when an error occurs during file transmission.
     *
     * @param rpcFileSenderWrapper The wrapper containing RPC file sender information and session details
     * @param errorMsg             The error message describing the failure
     */
    public void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {
        // Check if the listener is not null before proceeding
        if (rpcFileSenderListener != null && status[1] == 0) {
            status[1] = 1;
            DefaultVirtualThreadPool.execute(() -> rpcFileSenderListener.onFailure(rpcFileSenderWrapper, errorMsg));
        }
    }


    /**
     * Handles the process event for RPC file sending.
     * This method is called when there is a progress update during file transmission.
     * It executes the listener callback in a virtual thread pool if the listener is set.
     *
     * @param rpcFileSenderWrapper The wrapper containing RPC file sender information
     * @param process              The current file transmission process information
     */
    public void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {
        // Check if the file sender listener is not null
        if (rpcFileSenderListener != null) {
            // Execute the listener callback in a virtual thread pool
            // The isProcessOverride parameter determines whether the process can be overridden
            DefaultVirtualThreadPool.execute(isProcessOverride, () -> rpcFileSenderListener.onProcess(rpcFileSenderWrapper, process));
        }
    }

}
