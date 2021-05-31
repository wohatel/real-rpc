package com.murong.rpc.interaction;

public class RpcFileRequest extends RpcRequest {
    private boolean finished;//是否传输完毕
    private long position;  //当前传输的内容位置
    private long length;    //文件总大小
    private String hash;    //文件的摘要
    private String fileName;//文件名称
    private byte[] bytes;   //此次传输文件的大小

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }
}
