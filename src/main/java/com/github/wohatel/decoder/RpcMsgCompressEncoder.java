package com.github.wohatel.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author yaochuang
 */
public class RpcMsgCompressEncoder extends MessageToByteEncoder<ByteBuf> {

    private final EmbeddedChannel encoderChannel;

    public RpcMsgCompressEncoder(MessageToByteEncoder<ByteBuf> encoder) {
        encoderChannel = new EmbeddedChannel(encoder);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        // 读取是否压缩标志
        byte flag = in.readByte();
        // 标记位复位
        out.writeByte(flag);
        if (flag == 1) {
            System.out.println("开始压缩");
            // 压缩
            encoderChannel.writeOutbound(in.retain());
            ByteBuf buf;
            while ((buf = encoderChannel.readOutbound()) != null) {
                out.writeBytes(buf);
                buf.release(); // 必须 release，防止泄漏
            }
        } else {
            // 不压缩，直接原样输出
            out.writeBytes(in);
        }
    }
}