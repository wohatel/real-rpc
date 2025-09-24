package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import lombok.Getter;

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

}
