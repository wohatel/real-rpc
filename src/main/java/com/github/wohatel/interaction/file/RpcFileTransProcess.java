package com.github.wohatel.interaction.file;

import lombok.Getter;
import lombok.Setter;


/** * 文件传输进度控制
 */
@Getter
@Setter
public class RpcFileTransProcess {
    private Long startTime = System.currentTimeMillis();
    private Long fileLength;
    private Long sendSize;
    private Long remoteHandleSize;
    private Long startIndex;

    public RpcFileTransProcess copy() {
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        rpcFileTransProcess.setRemoteHandleSize(this.getRemoteHandleSize());
        rpcFileTransProcess.setFileLength(this.getFileLength());
        rpcFileTransProcess.setStartTime(this.getStartTime());
        rpcFileTransProcess.setSendSize(this.getSendSize());
        rpcFileTransProcess.setStartIndex(this.getStartIndex());
        return rpcFileTransProcess;
    }
}