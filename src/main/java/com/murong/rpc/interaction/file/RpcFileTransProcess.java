package com.murong.rpc.interaction.file;

import lombok.Getter;
import lombok.Setter;


/**
 * 文件传输进度控制
 */
@Getter
@Setter
public class RpcFileTransProcess {
    private Long startTime = System.currentTimeMillis();
    private Long fileSize;
    private Long sendSize;
    private Long remoteHandleSize;

    public RpcFileTransProcess copy() {
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        rpcFileTransProcess.setRemoteHandleSize(this.getRemoteHandleSize());
        rpcFileTransProcess.setFileSize(this.getFileSize());
        rpcFileTransProcess.setStartTime(this.getStartTime());
        rpcFileTransProcess.setSendSize(this.getSendSize());
        return rpcFileTransProcess;
    }
}