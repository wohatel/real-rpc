package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.ByteBufPoolManager;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.compression.Lz4FrameEncoder;
import io.netty.util.ReferenceCountUtil;
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
        // Step1: 构建原始 ByteBuf（未压缩）
        ByteBuf buffer = encodeMsg(ctx, msg);
        ByteBuf target = tryCompress(buffer, msg.isNeedCompress());
        out.add(target);
    }

    /**
     * 编码消息体到 ByteBuf
     * 1: 是否压缩
     * 2: 消息类型
     * 3: 消息长度
     * 4: 消息体
     * 5: 文件长度
     * 6: 文件
     */
    private ByteBuf encodeMsg(ChannelHandlerContext ctx, RpcMsg msg) {
        ByteBuf buffer = ctx.alloc().buffer();
        try {
            buffer.writeBoolean(msg.isNeedCompress()); // 1: 是否压缩
            buffer.writeInt(msg.getRpcCommandType().getCode()); // 2: 消息体类型
            // 消息体
            byte[] payloadBytes = JSON.toJSONBytes(msg.getPayload());
            buffer.writeInt(payloadBytes.length); // 3: 消息体长度
            buffer.writeBytes(payloadBytes);

            // 文件体
            ByteBuf fileBuffer = msg.getByteBuffer();
            int fileLength = fileBuffer == null ? 0 : fileBuffer.readableBytes();
            buffer.writeInt(fileLength); // 4: 文件长度
            if (fileLength > 0) {
                buffer.writeBytes(fileBuffer);
            }
            // 文件释放逻辑
            if (msg.getRpcCommandType() == RpcCommandType.file) {
                RpcFileRequest rpcFileRequest = msg.getPayload(RpcFileRequest.class);
                RpcSession rpcSession = rpcFileRequest.getRpcSession();
                ByteBufPoolManager.release(rpcSession.getSessionId(), fileBuffer);
                if (rpcFileRequest.isFinished()) {
                    ByteBufPoolManager.destory(rpcSession.getSessionId());
                }
            }
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(buffer);
            throw e;
        }
        return buffer;
    }

    /**
     * 压缩整个 ByteBuf，零拷贝，多线程安全
     */
    private ByteBuf tryCompress(ByteBuf input, boolean isCompress) {
        try {
            CompositeByteBuf composite = input.alloc().compositeBuffer();
            // 压缩标记位
            composite.addComponent(true, input.alloc().buffer(1).writeBoolean(isCompress));
            if (isCompress) {
                EmbeddedChannel ch = compressChannel.get();
                ch.writeOutbound(input.retain());
                ch.flushOutbound();
                ByteBuf buf;
                while ((buf = ch.readOutbound()) != null) {
                    composite.addComponent(true, buf);
                }
            } else {
                composite.addComponent(true, input.retain());
            }
            return composite;
        } finally {
            ReferenceCountUtil.safeRelease(input);
        }
    }
}