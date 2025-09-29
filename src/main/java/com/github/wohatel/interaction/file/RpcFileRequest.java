package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class RpcFileRequest extends RpcSessionRequest {
    private boolean finished;
    private long buffer;  //trans length every time
    private long serial;
    private int cacheBlock;
    private RpcFileInfo fileInfo;

    public RpcFileRequest(RpcSession rpcSession) {
        super(rpcSession);
    }
}
