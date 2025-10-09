package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.util.SnappyDirectByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author yaochuang
 */
@Slf4j
public class RpcMsgBodyDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        RpcMsg msg = new RpcMsg();
        boolean compressed = in.readBoolean();
        msg.setNeedCompress(compressed);
        int type = in.readInt();
        msg.setRpcCommandType(RpcCommandType.fromCode(type));
        byte[] payloadBytes = tryDeCompressPayload(msg, in);
        switch (msg.getRpcCommandType()) {
            case request, base -> msg.setPayload(JSON.parseObject(payloadBytes, RpcRequest.class));
            case session -> msg.setPayload(JSON.parseObject(payloadBytes, RpcSessionRequest.class));
            case response -> msg.setPayload(JSON.parseObject(payloadBytes, RpcResponse.class));
            case file -> {
                RpcFileRequest fileRequest = JSON.parseObject(payloadBytes, RpcFileRequest.class);
                msg.setPayload(fileRequest);
                tryDeCompressFileBuffer(ctx, msg, in);
            }
        }
        out.add(msg);
    }

    public byte[] tryDeCompressPayload(RpcMsg msg, ByteBuf in) {
        // 只有当不是file,并且需要压缩的时候才予以压缩
        int payloadLength = in.readInt();
        byte[] payloadBytes = new byte[payloadLength];
        in.readBytes(payloadBytes);
        if (msg.getRpcCommandType() != RpcCommandType.file && msg.isNeedCompress()) {
            return SnappyDirectByteBufUtil.decompress(payloadBytes);
        }
        return payloadBytes;
    }

    public void tryDeCompressFileBuffer(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf in) throws Exception {
        int fileLength = in.readInt();
        if (fileLength > 0) {
            if (msg.isNeedCompress()) {
                // 此处引入一个视图,不需要释放
                ByteBuf fileBuf = in.readSlice(fileLength);
                ByteBuf decompress = SnappyDirectByteBufUtil.decompress(ctx.alloc(), fileBuf);
                // decompress 需要下游释放
                msg.setByteBuffer(decompress);
            } else {
                // 此处引入一个引用,需要下游释放
                ByteBuf fileBuf = in.readRetainedSlice(fileLength);
                msg.setByteBuffer(fileBuf); // 下游负责 release
            }
        }
    }

}