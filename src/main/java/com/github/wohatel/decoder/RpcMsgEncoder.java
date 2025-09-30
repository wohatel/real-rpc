package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.ByteBufPoolManager;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.util.ReferenceByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.compression.Lz4FrameEncoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author yaochuang
 */
@Slf4j
public class RpcMsgEncoder extends MessageToMessageEncoder<RpcMsg> {

    // 每个线程一个 EmbeddedChannel，用于 LZ4 压缩
    private final ThreadLocal<EmbeddedChannel> compressChannel = ThreadLocal.withInitial(() -> new EmbeddedChannel(new Lz4FrameEncoder()));

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, List<Object> out) throws Exception {
        CompositeByteBuf outerBuffer = ctx.alloc().compositeBuffer();
        try {
            ByteBuf encodeBuf = encodeMsg(ctx, msg);
            outerBuffer.writeBoolean(msg.isNeedCompress());
            if (msg.isNeedCompress()) {
                ByteBuf compressBuf = tryCompress(encodeBuf);
                outerBuffer.addComponent(true, compressBuf);
            } else {
                outerBuffer.addComponent(true, encodeBuf);
            }
            out.add(outerBuffer);
        } catch (Throwable e) {
            ReferenceByteBufUtil.safeRelease(outerBuffer);
            log.error("rpcMsgEncoder encode error:", e);
            throw e;
        }
    }

    private ByteBuf encodeMsg(ChannelHandlerContext ctx, RpcMsg msg) {
        CompositeByteBuf buffer = ctx.alloc().compositeBuffer();
        try {
            buffer.writeBoolean(msg.isNeedCompress()); // 1: 是否压缩
            buffer.writeInt(msg.getRpcCommandType().getCode()); // 2: 消息体类型
            byte[] payloadBytes = JSON.toJSONBytes(msg.getPayload());
            buffer.writeInt(payloadBytes.length); // 3: 消息体长度
            buffer.writeBytes(payloadBytes);
            ByteBuf fileBuffer = msg.getByteBuffer();
            if (fileBuffer == null) {
                buffer.writeInt(0);
            } else {
                if (fileBuffer.refCnt() <= 0) {
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "fileBuffer is released");
                }
                buffer.addComponent(true, fileBuffer);
            }
            if (msg.getRpcCommandType() == RpcCommandType.file) {
                RpcFileRequest rpcFileRequest = msg.getPayload(RpcFileRequest.class);
                RpcSession rpcSession = rpcFileRequest.getRpcSession();
                ByteBufPoolManager.release(rpcSession.getSessionId(), fileBuffer);
                if (rpcFileRequest.isFinished()) {
                    ByteBufPoolManager.destory(rpcSession.getSessionId());
                }
            }
            return buffer;
        } catch (Throwable e) {
            ReferenceByteBufUtil.safeRelease(buffer);
            log.error("rpcMsgEncoder encodeMsg error:", e);
            throw e;
        }

    }

    private ByteBuf tryCompress(ByteBuf input) {
        CompositeByteBuf composite = input.alloc().compositeBuffer();
        try {
            EmbeddedChannel ch = compressChannel.get();
            ch.releaseOutbound();
            ch.writeOutbound(input);
            ch.flushOutbound();
            ByteBuf buf;
            while ((buf = ch.readOutbound()) != null) {
                composite.addComponent(true, buf);
            }
            return composite;
        } catch (Throwable e) {
            ReferenceByteBufUtil.safeRelease(input, composite);
            log.error("rpcMsgEncoder tryCompress error:", e);
            throw e;
        }
    }
}