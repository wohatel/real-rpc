package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.TransSessionManger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */
@RequiredArgsConstructor
@Getter
public class RpcFileReceiveWrapper {
    /**
     * 本地接受文件的位置
     */
    private final File file;

    private final RpcSession rpcSession;

    /**
     * 文件是否追加或续传
     */
    private final RpcFileTransModel transModel;

    /**
     * 发送放文件信息
     */
    private final RpcFileInfo rpcFileInfo;

    /**
     * 发送方的上下文信息
     */
    private final RpcSessionContext context;

    /**
     * 需要返送的总字节数
     */
    private final long needTransLength;

    /**
     * 中断的同时,可能触发文件接收异常事件
     * RpcFileSenderListener.onFailure()
     */
    public void interruptReceive() {
        TransSessionManger.release(rpcSession.getSessionId());
    }
}
