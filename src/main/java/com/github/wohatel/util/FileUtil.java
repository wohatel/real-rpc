package com.github.wohatel.util;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileUtil {

    /**     * File appendage or insertion
     */
    public static void appendFile(String file, byte[] bytes, long startPosition) throws IOException {
        try (FileChannel channel = FileChannel.open(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            channel.write(byteBuffer, startPosition);
        }
    }


    /**
     * The file is appended to the end
     *
     */
    public static void appendFile(String file, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            channel.write(byteBuffer);
        }
    }

    /**
     * md5 file hash
     *
     */
    @SneakyThrows
    public static String fileMd5Hash(File file) {
        return DigestUtils.md5Hex(new FileInputStream(file));
    }

    /**
     * The first headSize byte of the file is attempted,
     * and true is returned if the compression rate is lower than the specified threshold rate
     *
     * @param file     file
     * @param headSize Read the first headSize byte of the file
     * @param rate     (0-100)
     */
    @SneakyThrows
    public static boolean tryCompress(File file, int headSize, int rate) {
        if (rate < 0 || rate > 100) {
            throw new IllegalArgumentException("rate should in 0-100");
        }
        if (file.length() < headSize) {
            return false; // 文件太小，不处理
        }
        byte[] inputBytes = new byte[headSize];
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            dis.readFully(inputBytes);
        }
        byte[] compressed = SnappyDirectByteBufUtil.compress(inputBytes);
        double compressRate = compressed.length * 100.0 / headSize;
        log.info("file:" + file.getName() + " compressRate is " + compressRate + "%");
        return compressRate < rate;
    }

    @SneakyThrows
    public static String md5(File file) {
        return DigestUtils.md5Hex(new FileInputStream(file));
    }


}