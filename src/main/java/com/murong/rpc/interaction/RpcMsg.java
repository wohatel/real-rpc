package com.murong.rpc.interaction;

import java.util.Objects;

public class RpcMsg {
    private RpcCommandType rpcCommandType;
    private RpcRequest request;
    private RpcResponse response;
    private RpcFileRequest rpcFileRequest;

    public RpcMsg() {
    }

    public static RpcMsg build(Object object) {
        Objects.requireNonNull(object);
        if (object instanceof RpcFileRequest) {
            return new RpcMsg((RpcFileRequest) object);
        } else if (object instanceof RpcRequest) {
            return new RpcMsg((RpcRequest) object);
        } else if (object instanceof RpcResponse) {
            return new RpcMsg((RpcResponse) object);
        }
        throw new RuntimeException("类型不支持");
    }

    public RpcMsg(RpcRequest request) {
        rpcCommandType = RpcCommandType.request;
        this.request = request;
    }

    public RpcMsg(RpcResponse response) {
        rpcCommandType = RpcCommandType.response;
        this.response = response;
    }

    public RpcMsg(RpcFileRequest rpcFileRequest) {
        rpcCommandType = RpcCommandType.file;
        this.rpcFileRequest = rpcFileRequest;
    }

    public RpcCommandType getRpcCommandType() {
        return rpcCommandType;
    }


    public RpcFileRequest getRpcFileRequest() {
        return rpcFileRequest;
    }

    public void setRpcFileRequest(RpcFileRequest rpcFileRequest) {
        this.rpcFileRequest = rpcFileRequest;
    }

    public void setRpcCommandType(RpcCommandType rpcCommandType) {
        this.rpcCommandType = rpcCommandType;
    }

    public RpcRequest getRequest() {
        return request;
    }

    public void setRequest(RpcRequest request) {
        this.request = request;
    }

    public RpcResponse getResponse() {
        return response;
    }

    public void setResponse(RpcResponse response) {
        this.response = response;
    }
}
