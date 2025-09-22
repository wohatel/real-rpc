package com.github.wohatel.util;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
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

    /**
     * 文件追加或插入
     *
     * @param file          文件
     * @param bytes         数组
     * @param startPosition 开始位置
     * @throws IOException 异常
     */
    public static void appendFile(String file, byte[] bytes, long startPosition) throws IOException {
        try (FileChannel channel = FileChannel.open(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            channel.write(byteBuffer, startPosition);
        }
    }


    /**
     * 文件追加到末尾
     *
     */
    public static void appendFile(String file, byte[] bytes) throws IOException {
        try (FileChannel channel = FileChannel.open(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            channel.write(byteBuffer);
        }
    }

    /**
     * md5文件hash
     *
     */
    @SneakyThrows
    public static String fileMd5Hash(File file) {
        return DigestUtils.md5Hex(new FileInputStream(file));
    }

    /**
     * 尝试压缩文件前 headSize 个字节，若压缩率低于指定阈值 rate 则返回 true
     *
     * @param file     要压缩的文件
     * @param headSize 读取文件的前 headSize 字节
     * @param rate     压缩率阈值 (0-100)
     * @return 是否值得压缩（压缩率低于给定值）
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
        // LZ4 压缩
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor = factory.fastCompressor();
        int maxCompressedLength = compressor.maxCompressedLength(inputBytes.length);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedSize = compressor.compress(inputBytes, 0, inputBytes.length, compressed, 0, maxCompressedLength);
        double compressRate = compressedSize * 100.0 / headSize;
        log.info("file:" + file.getName() + " compressRate is " + compressRate + "%");
        return compressRate < rate;
    }

    @SneakyThrows
    public static String md5(File file) {
        return DigestUtils.md5Hex(new FileInputStream(file));
    }


}