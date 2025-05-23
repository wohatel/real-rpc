package com.murong.rpc.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.compression.JdkZlibEncoder;

public class RpcMsgCompressEncoder extends JdkZlibEncoder {


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        // 读取是否压缩标志
        byte flag = in.readByte();
        // 标记位复位
        out.writeByte(flag);
        if (flag == 1) {
            // 剩余的进行压缩
            super.encode(ctx, in, out);
        } else {
            // 不压缩，直接原样输出
            out.writeBytes(in);
        }
    }
}