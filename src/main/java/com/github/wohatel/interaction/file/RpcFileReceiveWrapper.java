package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import lombok.Getter;

import java.io.File;

/**
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

    private final File file;


    private final RpcFileTransModel transModel;


    private final RpcFileInfo rpcFileInfo;

    private final long needTransLength;

}
