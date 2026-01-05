package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.constant.RpcHeartAction;
import com.github.wohatel.interaction.base.RpcFileRequest;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcCommandType;
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

    /**
     * Decodes the incoming ByteBuf into a list of messages.
     *
     * @param ctx the ChannelHandlerContext which this handler belongs to
     * @param in  the ByteBuf from which to read data
     * @param out the list to which decoded messages should be added
     * @throws Exception if an error occurs during decoding
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Create a new RpcMsg object to store the decoded data
        RpcMsg msg = new RpcMsg();
        // Read and set the compression flag
        boolean compressed = in.readBoolean();
        msg.setNeedCompress(compressed);
        // Read and set the RPC command type
        int type = in.readInt();
        msg.setRpcCommandType(RpcCommandType.fromCode(type));
        // Attempt to decompress the payload based on the compression flag
        byte[] payloadBytes = tryDeCompressPayload(msg, in);
        // Process the payload based on the RPC command type
        switch (msg.getRpcCommandType()) {
            case request -> msg.setPayload(JSON.parseObject(payloadBytes, RpcRequest.class));
            case session -> msg.setPayload(JSON.parseObject(payloadBytes, RpcSessionRequest.class));
            case reaction -> msg.setPayload(JSON.parseObject(payloadBytes, RpcReaction.class));
            case heart -> msg.setPayload(JSON.parseObject(payloadBytes, RpcHeartAction.class));
            case file -> {
                RpcFileRequest fileRequest = JSON.parseObject(payloadBytes, RpcFileRequest.class);
                msg.setPayload(fileRequest);
                tryDeCompressFileBuffer(msg, in);
            }
        }
        // Add the decoded message to the output list
        out.add(msg);
    }

    /**
     * Attempts to decompress the payload of the RPC message if needed.
     *
     * @param msg the RpcMsg object containing message metadata
     * @param in  the ByteBuf from which to read data
     * @return the decompressed or original payload bytes
     * @throws IOException if an error occurs during decompression
     */
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

    /**
     * Attempts to decompress a file buffer from the input ByteBuf based on compression requirements.
     *
     * @param msg The RPC message containing compression flags and destination for the buffer
     * @param in  The input ByteBuf containing the file data to potentially decompress
     * @throws IOException If there's an error during decompression
     */
    public void tryDeCompressFileBuffer(RpcMsg msg, ByteBuf in) throws IOException {
        // Read the file length from the input buffer
        int fileLength = in.readInt();
        // Process only if there's actual file content
        if (fileLength > 0) {
            // Check if decompression is required based on message flag
            if (msg.isNeedCompress()) {
                // Read compressed bytes into array
                byte[] bytes = ByteBufUtil.readBytes(in, fileLength);
                // Decompress the bytes using Snappy
                byte[] decompress = Snappy.uncompress(bytes);
                // Set the decompressed buffer in the message
                msg.setByteBuffer(Unpooled.wrappedBuffer(decompress));
            } else {
                // No compression needed - directly wrap the bytes in a buffer
                ByteBuf fileBuf = Unpooled.wrappedBuffer(ByteBufUtil.readBytes(in, fileLength));
                msg.setByteBuffer(fileBuf); // 下游负责 release
            }
        }
    }

}