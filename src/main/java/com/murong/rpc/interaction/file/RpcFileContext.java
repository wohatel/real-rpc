package com.murong.rpc.interaction.file;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RpcFileContext {
    @Getter
    private final long recieveTime;
    @Getter
    private final long length;    //文件总大小
    @Getter
    private final String sessionId;    //文件标志
    @Getter
    private final String sourceFileName;//源文件名称
    @Getter
    private final JSONObject context;//发送端上下文信息

    @Getter
    private final long chunkHandleTimeOut; // 处理块超时时间
}
