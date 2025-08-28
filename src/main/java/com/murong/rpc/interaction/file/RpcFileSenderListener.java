package com.murong.rpc.interaction.file;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
public interface RpcFileSenderListener {

    /**
     * 传输成功触发
     */
    void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper);

    /**
     * 传输失败后触发
     */
    void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg);

    /**
     * 监控传输进程
     */
    void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process);

}
