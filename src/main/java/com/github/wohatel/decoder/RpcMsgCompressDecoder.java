package com.github.wohatel.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author yaochuang
 */
public class RpcMsgCompressDecoder extends ByteToMessageDecoder {

    private final EmbeddedChannel decoderChannel;

    public RpcMsgCompressDecoder(ByteToMessageDecoder decoder) {
        decoderChannel = new EmbeddedChannel(decoder);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 次数读取一个占位符[1] 还剩下一个[1] 留给下游的rpcMsgDecoder
        byte outerFlag = in.readByte();
        if (outerFlag == 1) {
            decoderChannel.writeInbound(in.retain());
            ByteBuf o;
            while ((o = decoderChannel.readInbound()) != null) {
                out.add(o);
            }
        } else {
            out.add(in.readRetainedSlice(in.readableBytes()));
        }
    }

}