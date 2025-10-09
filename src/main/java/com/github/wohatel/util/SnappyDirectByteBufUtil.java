package com.github.wohatel.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SnappyDirectByteBufUtil {


    /**
     * 压缩 byte[] 数组
     */
    public static byte[] compress(byte[] input) {
        try {
            return Snappy.compress(input);
        } catch (IOException e) {
            throw new RuntimeException("Snappy compress failed", e);
        }
    }

    /**
     * 解压 byte[] 数组
     */
    public static byte[] decompress(byte[] input) {
        try {
            return Snappy.uncompress(input);
        } catch (IOException e) {
            throw new RuntimeException("Snappy decompress failed", e);
        }
    }

    public static ByteBuf compress(ByteBufAllocator allocator, ByteBuf input) throws Exception {
        int originalSize = input.readableBytes();
        ByteBuffer srcBuffer = input.internalNioBuffer(input.readerIndex(), originalSize).slice();

        int maxCompressedSize = Snappy.maxCompressedLength(originalSize);
        ByteBuf out = allocator.directBuffer(4 + maxCompressedSize);
        try {
            out.writeInt(originalSize);
            ByteBuffer dstBuffer = out.internalNioBuffer(out.writerIndex(), maxCompressedSize).slice();

            int compressedSize = Snappy.compress(srcBuffer, dstBuffer);
            out.writerIndex(out.writerIndex() + compressedSize);
            return out;
        } catch (Exception e) {
            out.release();
            throw e;
        }
    }

    public static ByteBuf decompress(ByteBufAllocator allocator, ByteBuf input) throws Exception {
        if (input == null || input.readableBytes() < 4)
            throw new IllegalArgumentException("Input buffer too small");

        int originalSize = input.readInt();
        int compressedSize = input.readableBytes();
        ByteBuffer srcBuffer = input.internalNioBuffer(input.readerIndex(), compressedSize).slice();

        ByteBuf out = allocator.directBuffer(originalSize);
        try {
            ByteBuffer dstBuffer = out.internalNioBuffer(out.writerIndex(), originalSize).slice();

            int decompressedSize = Snappy.uncompress(srcBuffer, dstBuffer);
            out.writerIndex(out.writerIndex() + decompressedSize);
            return out;
        } catch (Exception e) {
            out.release();
            throw e;
        }
    }

    
}