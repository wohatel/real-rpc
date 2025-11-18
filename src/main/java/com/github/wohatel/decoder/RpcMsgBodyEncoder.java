package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.ByteBufPoolManager;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.base.RpcFileRequest;
import com.github.wohatel.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * @author yaochuang
 */
@Slf4j
public class RpcMsgBodyEncoder extends MessageToByteEncoder<RpcMsg> {

    /**
     * Encodes the RpcMsg into a ByteBuf for transmission.
     *
     * @param ctx the ChannelHandlerContext which this handler belongs to
     * @param msg the RpcMsg to encode
     * @param out the ByteBuf into which the encoded message will be written
     * @throws Exception if an error occurs during encoding
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, ByteBuf out) throws Exception {
        out.writeBoolean(msg.isNeedCompress()); // 1: 是否压缩
        out.writeInt(msg.getRpcCommandType().getCode()); // 2: 消息体类型
        // 压缩消息体
        tryCompressPayload(msg, out);
        if (msg.getRpcCommandType() == RpcCommandType.file) {
            tryCompressFileBuffer(msg, out);
        }
    }

    /**
     * Method to attempt compression of the payload in an RPC message.
     * This method checks if the payload needs compression and is not a file type before compressing.
     *
     * @param msg The RPC message containing the payload to be compressed
     * @param out The ByteBuf to write the compressed or uncompressed payload to
     * @throws IOException If an I/O error occurs during the compression or writing process
     */
    public void tryCompressPayload(RpcMsg msg, ByteBuf out) throws IOException {
        // 只有当不是file,并且需要压缩的时候才予以压缩
        byte[] payloadBytes = JSON.toJSONBytes(msg.getPayload());
        if (msg.getRpcCommandType() != RpcCommandType.file && msg.isNeedCompress()) {
            byte[] compress = Snappy.compress(payloadBytes);
            out.writeInt(compress.length);
            out.writeBytes(compress);
        } else {
            out.writeInt(payloadBytes.length); // 3: 消息体长度
            out.writeBytes(payloadBytes);
        }
    }

    /**
     * Method to try compressing a file buffer and write it to the output buffer.
     * This method handles the compression of file data if needed, writes the appropriate
     * header information, and manages the release of resources.
     *
     * @param msg The RpcMsg containing the file buffer and compression flag
     * @param out The output ByteBuf to write the compressed or uncompressed data to
     * @throws IOException If an I/O error occurs during compression or writing
     */
    public void tryCompressFileBuffer(RpcMsg msg, ByteBuf out) throws IOException {
        // Get the file buffer from the RpcMsg
        ByteBuf fileBuffer = msg.getByteBuffer();
        if (fileBuffer == null) {
            // If no file buffer, write 0 as length indicator
            out.writeInt(0);
        } else {
            // Check if compression is needed
            if (msg.isNeedCompress()) {
                // Compress the file buffer using Snappy
                byte[] bytes = Snappy.compress(ByteBufUtil.readBytes(fileBuffer));
                // Write the compressed data length and the compressed data itself
                out.writeInt(bytes.length);
                out.writeBytes(bytes);
            } else {
                // If no compression needed, write the original buffer size and data
                out.writeInt(fileBuffer.readableBytes());
                out.writeBytes(fileBuffer);
            }
        }
        // Get the RPC file request and session from the message payload
        RpcFileRequest rpcFileRequest = msg.getPayload(RpcFileRequest.class);
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        // Release the file buffer back to the pool
        ByteBufPoolManager.release(rpcSession.getSessionId(), fileBuffer);
        // If this is the last block of the file, destroy the session's pool
        if (rpcFileRequest.isLastBlock()) {
            ByteBufPoolManager.destroy(rpcSession.getSessionId());
        }
    }

}