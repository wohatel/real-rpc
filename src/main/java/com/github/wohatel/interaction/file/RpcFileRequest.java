package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString(callSuper = true)
@Accessors(chain = true)
public class RpcFileRequest extends RpcSessionRequest {
    private boolean lastBlock; // 是否是最后一块
    private long blockSize;  //trans length every time
    private long serial;

    public RpcFileRequest(RpcSession rpcSession) {
        super(rpcSession);
    }
}
