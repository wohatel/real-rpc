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
        byte outerFlag = in.readByte(); // 第一个标志位
        if (outerFlag == 1) {
            System.out.println("开始解压");
            // 由于有两个标记位,读完第一个后,进行解压后out的结构是[1][body]
            decoderChannel.writeInbound(in.retain());
            ByteBuf o;
            while ((o = decoderChannel.readInbound()) != null) {
                out.add(o);
            }
        } else {
            // 由于有两个标记位,读完第一个后,不解压数据直接就是 [0][body]
            out.add(in.readRetainedSlice(in.readableBytes()));
        }
    }

}