package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.ByteBufPoolManager;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.util.SnappyDirectByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaochuang
 */
@Slf4j
public class RpcMsgBodyEncoder extends MessageToByteEncoder<RpcMsg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf out) throws Exception {
        out.writeBoolean(msg.isNeedCompress()); // 1: 是否压缩
        out.writeInt(msg.getRpcCommandType().getCode()); // 2: 消息体类型
        // 压缩消息体
        tryCompressPayload(msg, out);
        if (msg.getRpcCommandType() == RpcCommandType.file) {
            tryCompressFileBuffer(ctx, msg, out);
        }
    }

    public void tryCompressPayload(RpcMsg msg, ByteBuf out) {
        // 只有当不是file,并且需要压缩的时候才予以压缩
        byte[] payloadBytes = JSON.toJSONBytes(msg.getPayload());
        if (msg.getRpcCommandType() != RpcCommandType.file && msg.isNeedCompress()) {
            byte[] compress = SnappyDirectByteBufUtil.compress(payloadBytes);
            out.writeInt(compress.length);
            out.writeBytes(compress);
        } else {
            out.writeInt(payloadBytes.length); // 3: 消息体长度
            out.writeBytes(payloadBytes);
        }
    }

    public void tryCompressFileBuffer(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf out) throws Exception {
        ByteBuf fileBuffer = msg.getByteBuffer();
        if (fileBuffer == null) {
            out.writeInt(0);
        } else {
            if (msg.isNeedCompress()) {
                ByteBuf compress = SnappyDirectByteBufUtil.compress(ctx.alloc(), fileBuffer.slice());
                out.writeInt(compress.readableBytes());
                out.writeBytes(compress);
                compress.release();

            } else {
                out.writeInt(fileBuffer.readableBytes());
                out.writeBytes(fileBuffer);
            }
        }
        RpcFileRequest rpcFileRequest = msg.getPayload(RpcFileRequest.class);
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        ByteBufPoolManager.release(rpcSession.getSessionId(), fileBuffer);
        if (rpcFileRequest.isFinished()) {
            ByteBufPoolManager.destory(rpcSession.getSessionId());
        }
    }

}