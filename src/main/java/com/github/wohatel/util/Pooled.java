package com.github.wohatel.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

/**
 * description
 *
 * @author yaochuang 2025/09/22 18:19
 */
public class Pooled {

    public static ByteBuf wrappedBuffer(byte[] bytes) {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer(bytes.length);
        buf.writeBytes(bytes);
        return buf;
    }

    public static ByteBuf copiedBuffer(CharSequence charSequence) {
        return wrappedBuffer(charSequence.toString().getBytes(CharsetUtil.UTF_8));
    }

    public static ByteBuf copiedBuffer(String charSequence, Charset charset) {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
        buf.writeCharSequence(charSequence, charset);
        return buf;
    }
}
