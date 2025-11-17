package com.github.wohatel.interaction.file;

import lombok.Getter;
import lombok.Setter;


/**
 * A class representing the process of file transfer in RPC (Remote Procedure Call) communication.
 * This class tracks various metrics and states during the file transfer process.
 */
@Getter
@Setter
public class RpcFileTransProcess {
    // The timestamp when the file transfer process started
    private Long startTime = System.currentTimeMillis();
    // The total length of the file being transferred
    private Long fileLength;
    // The amount of data that has been sent
    private Long sendSize;
    // The amount of data that has been handled on the remote side
    private Long remoteHandleSize;
    // The starting index from which the file transfer began
    private Long startIndex;

    /**
     * Creates a copy of the current RpcFileTransProcess instance.
     * This method is useful when you need to create a snapshot of the transfer process
     * without modifying the original instance.
     *
     * @return A new RpcFileTransProcess instance with all values copied from the original
     */
    public RpcFileTransProcess copy() {
        // Create a new instance of RpcFileTransProcess
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        // Copy all the field values from the current instance to the new instance
        rpcFileTransProcess.setRemoteHandleSize(this.getRemoteHandleSize());
        rpcFileTransProcess.setFileLength(this.getFileLength());
        rpcFileTransProcess.setStartTime(this.getStartTime());
        rpcFileTransProcess.setSendSize(this.getSendSize());
        rpcFileTransProcess.setStartIndex(this.getStartIndex());
        // Return the copied instance
        return rpcFileTransProcess;
    }
}