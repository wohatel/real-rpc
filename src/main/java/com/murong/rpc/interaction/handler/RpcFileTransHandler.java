package com.murong.rpc.interaction.handler;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.file.RpcFileTransProcess;

import java.io.File;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
public interface RpcFileTransHandler {

    /**
     * 传输进度
     * rpcFileTransProcess 传输文件的进度
     */
    default void onProcess(final File file, final JSONObject context, final RpcFileTransProcess rpcFileTransProcess) {

    }

    /**
     * 文件向远端传输过程中,远端回调错误
     * errorMsg 错误信息
     */
    default void onRemoteFailure(final File file, final JSONObject context, String errorMsg) {

    }

    /**
     * 文件向远端传输过程中,本地执行出错
     * errorMsg 错误信息
     */
    default void onLocalFailure(final File file, final JSONObject context, String errorMsg) {

    }

    /**
     * 文件传输完成
     * errorMsg 错误信息
     */
    void onSuccess(final File file, final JSONObject context);

}
