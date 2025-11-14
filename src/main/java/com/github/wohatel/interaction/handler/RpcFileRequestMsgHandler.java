package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileSignature;

/**
 * 文件接收端处理事件接口
 *
 * @author yaochuang 2025/03/28 09:44
 */
public interface RpcFileRequestMsgHandler {

    /**
     * Determine the file storage location (called before the file transfer starts)
     */
    RpcFileSignature getTargetFile(final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo);

    /**
     * Remotely receive the progress of the file, which is triggered every time there is a change
     */
    default void onProcess(final RpcFileReceiveWrapper rpcFileWrapper, long receivedSize,final RpcFileInterrupter interrupter) {

    }

    /**
     * File Receiving Exception Execution
     *
     */
    default void onFailure(final RpcFileReceiveWrapper rpcFileWrapper, final Exception e) {

    }

    default void onSuccess(final RpcFileReceiveWrapper rpcFileWrapper) {

    }

    /**
     * while session started, this method will be exec in last
     */
    default void onFinally(final RpcFileReceiveWrapper rpcFileWrapper) {

    }
}