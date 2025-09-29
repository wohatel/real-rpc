package com.github.wohatel.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/** * decode to TypeReference<T>
 *
 * @author yaochuang 2025/09/17 17:06
 */
public class ByteBufDecoder {
    @SuppressWarnings("unchecked")
    public static <T> T decode(ByteBuf buf, TypeReference<T> typeRef) throws IOException {
        Type type = typeRef.getType();
        if (type == String.class) {
            return (T) buf.toString(StandardCharsets.UTF_8);
        }
        if (type == byte[].class) {
            byte[] array = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), array); // 不改变 readerIndex
            return (T) array;
        }
        return JSON.parseObject(buf.toString(StandardCharsets.UTF_8), type);
    }
}
