package com.github.wohatel.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.Lz4FrameEncoder;

import java.nio.charset.StandardCharsets;

/**
 * description
 *
 * @author yaochuang 2025/09/22 16:39
 */
public class EncoderUtil {

    public static String encode() {
        Lz4FrameEncoder encoder = new Lz4FrameEncoder();
        EmbeddedChannel ch = new EmbeddedChannel(encoder);
        ByteBuf directBuf = ByteBufAllocator.DEFAULT.directBuffer(1024);
        directBuf.writeBytes("Hello World".getBytes(StandardCharsets.UTF_8));
        ch.writeOutbound(directBuf.retain());
        ByteBuf compressed = ch.readOutbound();
        return null;
    }


}
