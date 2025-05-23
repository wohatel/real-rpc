package com.murong.rpc.util;


import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.xerial.snappy.Snappy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class FileUtil {

    /**
     * 文件追加或插入
     *
     * @param file
     * @param bytes
     * @param startPosition
     * @throws IOException
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
     * @param file
     * @param bytes
     * @throws IOException
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
     * @param file
     * @return
     * @throws IOException
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
     * @throws IOException 读取或压缩异常
     */
    @SneakyThrows
    public static boolean tryCompress(File file, int headSize, int rate) {
        if (rate < 0 || rate > 100) {
            throw new IllegalArgumentException("rate 应该在 0-100 之间");
        }
        if (file.length() < headSize) {
            return false; // 文件太小，不处理
        }

        byte[] inputBytes = new byte[headSize];
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            dis.readFully(inputBytes); // 确保读满
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater)) {
            dos.write(inputBytes);
        }

        double compressRate = baos.size() * 100.0 / headSize;
        return compressRate < rate;
    }

}