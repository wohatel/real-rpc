package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.util.ReferenceByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.compression.Lz4FrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author yaochuang
 */
@Slf4j
public class RpcMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final ThreadLocal<EmbeddedChannel> decompressChannel = ThreadLocal.withInitial(() -> new EmbeddedChannel(new Lz4FrameDecoder()));

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        boolean isCompress = in.readBoolean();
        ByteBuf retain = in.retain();
        if (isCompress) {
            ByteBuf decompressBuf = tryDecompress(retain);
            RpcMsg rpcMsg = decodeMsg(decompressBuf);
            out.add(rpcMsg);
        } else {
            RpcMsg rpcMsg = decodeMsg(retain);
            out.add(rpcMsg);
        }

    }

    private RpcMsg decodeMsg(ByteBuf input) {
        try {
            RpcMsg msg = new RpcMsg();
            boolean compressed = input.readBoolean();
            msg.setNeedCompress(compressed);
            int type = input.readInt();
            msg.setRpcCommandType(RpcCommandType.fromCode(type));
            int payloadLength = input.readInt();
            byte[] payloadBytes = new byte[payloadLength];
            input.readBytes(payloadBytes);
            int fileLength = input.readInt();
            switch (msg.getRpcCommandType()) {
                case request, base -> msg.setPayload(JSON.parseObject(payloadBytes, RpcRequest.class));
                case session -> msg.setPayload(JSON.parseObject(payloadBytes, RpcSessionRequest.class));
                case response -> msg.setPayload(JSON.parseObject(payloadBytes, RpcResponse.class));
                case file -> {
                    RpcFileRequest fileRequest = JSON.parseObject(payloadBytes, RpcFileRequest.class);
                    msg.setPayload(fileRequest);
                    if (fileLength > 0) {
                        ByteBuf fileBuf = input.readRetainedSlice(fileLength);
                        msg.setByteBuffer(fileBuf); // 下游负责 release
                    }
                }
            }
            return msg;
        } finally {
            ReferenceByteBufUtil.safeRelease(input);
        }
    }

    private ByteBuf tryDecompress(ByteBuf byteBuf) {
        CompositeByteBuf decompressed = byteBuf.alloc().compositeBuffer();
        try {
            EmbeddedChannel ch = decompressChannel.get();
            ch.releaseInbound();
            ch.writeInbound(byteBuf);
            ch.flushInbound();
            ByteBuf buf;
            while ((buf = ch.readInbound()) != null) {
                decompressed.addComponent(true, buf);
            }
            return decompressed;
        } catch (Throwable e) {
            ReferenceByteBufUtil.safeRelease(decompressed, byteBuf);
            log.error("RpcMsgDecoder tryDecompress exception:", e);
            throw e;
        }
    }
}