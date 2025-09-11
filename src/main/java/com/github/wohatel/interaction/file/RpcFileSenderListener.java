package com.github.wohatel.interaction.file;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
public interface RpcFileSenderListener {

    /**
     * 传输成功触发
     */
    default void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {

    }

    /**
     * 传输失败后触发
     */
    default void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {

    }

    /**
     * 监控传输进程
     */
    default void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {
    }

}
