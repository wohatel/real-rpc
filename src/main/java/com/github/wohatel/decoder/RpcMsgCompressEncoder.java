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
        // 此处只获取,不改变readIndex,变成双占位符[1][1] 或者[0][0]
        byte flag = in.getByte(0);
        out.writeByte(flag);
        if (flag == 1) {
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