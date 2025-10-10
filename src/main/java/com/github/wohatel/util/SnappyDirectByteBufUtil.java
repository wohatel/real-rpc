package com.github.wohatel.util;

import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

import java.io.IOException;

@Slf4j
public class SnappyDirectByteBufUtil {


    /**
     * 压缩 byte[] 数组
     */
    public static byte[] compress(byte[] input) {
        try {
            return Snappy.compress(input);
        } catch (IOException e) {
            log.error("Snappy compress:", e);
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
            log.error("Snappy decompress:", e);
            throw new RuntimeException("Snappy decompress failed", e);
        }
    }

}