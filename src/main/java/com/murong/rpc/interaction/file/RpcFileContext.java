package com.murong.rpc.interaction.file;


import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RpcFileContext {
    @Getter
    private final long recieveTime;
    @Getter
    private final long length;    //文件总大小
    @Getter
    private final String sourceFileName;//源文件名称
    @Getter
    private final RpcSession rpcSession;
    @Getter
    private final RpcSessionContext context;
}
