package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */

@Getter
public class RpcFileReceiveWrapper extends RpcSessionContextWrapper {

    public RpcFileReceiveWrapper(RpcSession rpcSession, RpcSessionContext context, File file, RpcFileTransModel transModel, RpcFileInfo rpcFileInfo, long needTransLength) {
        super(rpcSession, context);
        this.file = file;
        this.transModel = transModel;
        this.rpcFileInfo = rpcFileInfo;
        this.needTransLength = needTransLength;
    }

    /**
     * 本地接受文件的位置
     */
    private final File file;

    /**
     * 文件是否追加或续传
     */
    private final RpcFileTransModel transModel;

    /**
     * 发送放文件信息
     */
    private final RpcFileInfo rpcFileInfo;

    /**
     * 需要返送的总字节数
     */
    private final long needTransLength;


    /**
     * 中断的同时,可能触发文件接收异常事件
     * RpcFileSenderListener.onFailure()
     */
    public void interruptReceive() {
        RpcSessionTransManger.release(rpcSession.getSessionId());
    }
}
