package com.github.wohatel.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.ByteBuf;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;


/**
 * A utility class for ByteBuf operations.
 * This class provides methods for decoding ByteBuf to various types and reading bytes from ByteBuf.
 * The class is designed with a private constructor to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ByteBufUtil {
    /**
     * Decodes a ByteBuf to the specified type.
     *
     * @param <T>     The type to decode to
     * @param buf     The ByteBuf to decode
     * @param typeRef The type reference to decode to
     * @return The decoded object of type T
     */
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

    /**
     * Reads all bytes from a ByteBuf into a byte array.
     *
     * @param buf The ByteBuf to read from
     * @return A byte array containing all bytes from the ByteBuf, or null if the input is null
     */
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

    /**
     * Reads a specified number of bytes from a ByteBuf into a byte array.
     *
     * @param buf    The ByteBuf to read from
     * @param length The number of bytes to read
     * @return A byte array containing the specified number of bytes from the ByteBuf
     */
    public static byte[] readBytes(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }
}
