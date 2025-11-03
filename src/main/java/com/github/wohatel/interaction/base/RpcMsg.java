package com.github.wohatel.interaction.base;

import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import java.util.Objects;

/**
 * @author yaochuang
 */
@Data
public class RpcMsg {
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
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.request, request);
        rpcMsg.setNeedCompress(request.isEnableCompress());
        return rpcMsg;
    }

    public static RpcMsg fromSessionRequest(RpcSessionRequest rpcSessionRequest) {
        Objects.requireNonNull(rpcSessionRequest);
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.session, rpcSessionRequest);
        rpcMsg.setNeedCompress(rpcSessionRequest.isEnableCompress());
        return rpcMsg;
    }

    public static RpcMsg fromReaction(RpcReaction reaction) {
        Objects.requireNonNull(reaction);
        return new RpcMsg(RpcCommandType.reaction, reaction);
    }

    public static RpcMsg fromFileRequest(RpcFileRequest fileRequest) {
        Objects.requireNonNull(fileRequest);
        RpcMsg rpcMsg = new RpcMsg(RpcCommandType.file, fileRequest);
        rpcMsg.setNeedCompress(fileRequest.isEnableCompress());
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
