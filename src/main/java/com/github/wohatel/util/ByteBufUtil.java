package com.github.wohatel.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/** * decode to TypeReference<T>
 *
 * @author yaochuang 2025/09/17 17:06
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ByteBufUtil {
    @SuppressWarnings("unchecked")
    public static <T> T decode(ByteBuf buf, TypeReference<T> typeRef) {
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

    public static byte[] readBytes(ByteBuf buf) {
        if (buf == null) return null;
        int len = buf.readableBytes();
        byte[] array = new byte[len];
        if (buf.hasArray()) {
            // 计算真实偏移
            int offset = buf.arrayOffset() + buf.readerIndex();
            System.arraycopy(buf.array(), offset, array, 0, len);
        } else {
            // direct buffer 或 slice，安全拷贝
            buf.getBytes(buf.readerIndex(), array);
        }
        return array;
    }

    public static byte[] readBytes(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }
}
