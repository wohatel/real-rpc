package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RpcFileRequest extends RpcSessionRequest {
    private boolean finished;//是否传输完毕
    private long buffer;  //每次传输的大小
    private long serial;  // 编号
    private long length;    //文件总大小
    private int cacheBlock; // 缓存块大小
    private String fileName;//文件名称

    public RpcFileRequest(RpcSession rpcSession) {
        super(rpcSession);
    }
}
