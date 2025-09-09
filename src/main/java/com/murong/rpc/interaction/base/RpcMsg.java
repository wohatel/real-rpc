package com.murong.rpc.interaction.base;

import com.murong.rpc.interaction.constant.RpcCommandType;
import com.murong.rpc.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Objects;

/**
 * @author yaochuang
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RpcMsg extends AbstractCompressAble {
    private RpcCommandType rpcCommandType;
    private Object payload;
    private ByteBuf byteBuffer;

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
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.request, request);
        rpcMsg.setNeedCompress(request.isNeedCompress());
        return rpcMsg;
    }

    public static RpcMsg fromSessionRequest(RpcSessionRequest rpcSessionRequest) {
        Objects.requireNonNull(rpcSessionRequest);
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.session, rpcSessionRequest);
        rpcMsg.setNeedCompress(rpcSessionRequest.isNeedCompress());
        return rpcMsg;
    }

    public static RpcMsg fromResponse(RpcResponse response) {
        Objects.requireNonNull(response);
        return new RpcMsg(RpcCommandType.response, response);
    }

    public static RpcMsg fromFileRequest(RpcFileRequest fileRequest) {
        Objects.requireNonNull(fileRequest);
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.file, fileRequest);
        rpcMsg.setNeedCompress(fileRequest.isNeedCompress());
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

}
