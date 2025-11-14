package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

import java.io.IOException;
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
            case request -> msg.setPayload(JSON.parseObject(payloadBytes, RpcRequest.class));
            case session -> msg.setPayload(JSON.parseObject(payloadBytes, RpcSessionRequest.class));
            case reaction -> msg.setPayload(JSON.parseObject(payloadBytes, RpcReaction.class));
            case file -> {
                RpcFileRequest fileRequest = JSON.parseObject(payloadBytes, RpcFileRequest.class);
                msg.setPayload(fileRequest);
                tryDeCompressFileBuffer(msg, in);
            }
        }
        out.add(msg);
    }

    public byte[] tryDeCompressPayload(RpcMsg msg, ByteBuf in) throws IOException {
        // 只有当不是file,并且需要压缩的时候才予以压缩
        int payloadLength = in.readInt();
        byte[] payloadBytes = new byte[payloadLength];
        in.readBytes(payloadBytes);
        if (msg.getRpcCommandType() != RpcCommandType.file && msg.isNeedCompress()) {
            return Snappy.uncompress(payloadBytes);
        }
        return payloadBytes;
    }

    public void tryDeCompressFileBuffer(RpcMsg msg, ByteBuf in) throws IOException {
        int fileLength = in.readInt();
        if (fileLength > 0) {
            if (msg.isNeedCompress()) {
                byte[] bytes = ByteBufUtil.readBytes(in, fileLength);
                byte[] decompress = Snappy.uncompress(bytes);
                msg.setByteBuffer(Unpooled.wrappedBuffer(decompress));
            } else {
                ByteBuf fileBuf = Unpooled.wrappedBuffer(ByteBufUtil.readBytes(in, fileLength));
                msg.setByteBuffer(fileBuf); // 下游负责 release
            }
        }
    }

}