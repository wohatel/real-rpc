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

import java.util.List;

/** * @author yaochuang
 */
public class RpcMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    private final ThreadLocal<EmbeddedChannel> decompressChannel = ThreadLocal.withInitial(() -> new EmbeddedChannel(new Lz4FrameDecoder()));

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        ByteBuf byteBuf = tryDecompress(in);
        ReferenceByteBufUtil.finallyRelease(() -> {
            RpcMsg rpcMsg = decodeMsg(byteBuf);
            out.add(rpcMsg);
        }, byteBuf);
    }

    private RpcMsg decodeMsg(ByteBuf input) {
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
    }

    private ByteBuf tryDecompress(ByteBuf in) {
        boolean isCompress = in.readBoolean();
        ByteBuf byteBuf = in.retain();
        if (!isCompress) {
            return byteBuf;
        }
        CompositeByteBuf decompressed = in.alloc().compositeBuffer();
        return ReferenceByteBufUtil.exceptionRelease(() -> {
            EmbeddedChannel ch = decompressChannel.get();
            ch.finishAndReleaseAll();
            ch.writeInbound(byteBuf);
            ch.flushInbound();
            ByteBuf buf;
            while ((buf = ch.readInbound()) != null) {
                decompressed.addComponent(true, buf);
            }
            return decompressed;
        }, decompressed, byteBuf);
    }
}