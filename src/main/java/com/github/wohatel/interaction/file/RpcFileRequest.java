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
    private boolean finished;//是否传输完毕
    private long buffer;  //每次传输的大小
    private long serial;  // 编号
    private int cacheBlock; // 缓存块大小
    private RpcFileInfo fileInfo;
    public RpcFileRequest(RpcSession rpcSession) {
        super(rpcSession);
    }
}
