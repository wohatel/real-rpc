package com.murong.rpc.interaction.base;

import com.murong.rpc.interaction.constant.RpcCommandType;
import com.murong.rpc.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.Getter;

import java.util.Objects;

/**
 * @author yaochuang
 */
@Getter
@Data
public class RpcMsg {
    // Getters and Setters
    private RpcCommandType rpcCommandType;
    private Object payload;
    private ByteBuf byteBuffer;
    private boolean needCompress;

    public RpcMsg() {
    }

    public RpcMsg(RpcCommandType type, Object payload) {
        this.rpcCommandType = type;
        this.payload = payload;
    }

    public static RpcMsg fromRequest(RpcRequest request) {
        Objects.requireNonNull(request);
        if (request instanceof RpcFileRequest rpcFileRequest) {
            return fromFileRequest(rpcFileRequest);
        }
        if (request instanceof RpcSessionRequest rpcSessionRequest) {
            return fromSessionRequest(rpcSessionRequest);
        }
        return new RpcMsg(RpcCommandType.request, request);
    }

    public static RpcMsg fromSessionRequest(RpcSessionRequest rpcSessionRequest) {
        Objects.requireNonNull(rpcSessionRequest);
        return new RpcMsg(RpcCommandType.session, rpcSessionRequest);
    }

    public static RpcMsg fromResponse(RpcResponse response) {
        Objects.requireNonNull(response);
        return new RpcMsg(RpcCommandType.response, response);
    }

    public static RpcMsg fromFileRequest(RpcFileRequest fileRequest) {
        Objects.requireNonNull(fileRequest);
        return new RpcMsg(RpcCommandType.file, fileRequest);
    }

    public static RpcMsg fromHeart() {
        RpcMsg rpcMsg = new RpcMsg();
        rpcMsg.setRpcCommandType(RpcCommandType.heart);
        rpcMsg.setNeedCompress(false);
        return rpcMsg;
    }

    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        if (payload == null) {
            return null;
        }
        if (!clazz.isInstance(payload)) {
            throw new IllegalStateException("Payload is not of type " + clazz.getName());
        }
        return (T) payload;
    }

    public void setRpcCommandType(RpcCommandType rpcCommandType) {
        this.rpcCommandType = rpcCommandType;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public void setByteBuffer(ByteBuf byteBuffer) {
        this.byteBuffer = byteBuffer;
    }
}
