package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.SneakyThrows;

import java.util.List;

/**
 * @author yaochuang
 */
public class RpcMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        RpcMsg msg = new RpcMsg();
        byte isCompress = in.readByte();
        msg.setNeedCompress(isCompress == 1);
        byte type = in.readByte(); // **读取消息类型**
        msg.setRpcCommandType(RpcCommandType.fromCode(type));
        switch (msg.getRpcCommandType()) {
            case request, base -> msg.setPayload(readPayload(in, RpcRequest.class));
            case session -> msg.setPayload(readPayload(in, RpcSessionRequest.class));
            case response -> msg.setPayload(readPayload(in, RpcResponse.class));
            case file -> {
                RpcFileRequest fileRequest = readPayload(in, RpcFileRequest.class);
                msg.setPayload(fileRequest);
                // 读取文件 ByteBuf
                int fileLength = in.readInt();
                if (fileLength > 0) {
                    ByteBuf fileBuf = in.readRetainedSlice(fileLength);
                    msg.setByteBuffer(fileBuf); // 下游负责 release
                }
            }
            default -> {
                return; // 未知类型直接丢弃
            }
        }
        out.add(msg);
    }

    /**
     * 将payload读取出来
     */
    @SneakyThrows
    public <T> T readPayload(ByteBuf buffer, Class<T> clazz) {
        int length = buffer.readInt();
        if (length <= 0) {
            return null;
        }

        ByteBuf payloadSlice = buffer.readRetainedSlice(length);
        try (ByteBufInputStream inputStream = new ByteBufInputStream(payloadSlice, false)) {
            return JSON.parseObject(inputStream, clazz); // FastJSON2 自动识别 UTF-8
        } finally {
            payloadSlice.release();
        }
    }
}