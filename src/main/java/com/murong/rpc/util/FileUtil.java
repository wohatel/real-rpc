package com.murong.rpc.util;


import org.springframework.util.DigestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
        try (
                FileChannel channel = FileChannel.open(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        ) {
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
        try (
                FileChannel channel = FileChannel.open(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        ) {
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
    public static String fileMd5Hash(String file) {

        try {
            return DigestUtils.md5DigestAsHex(new FileInputStream(file));
        } catch (IOException e) {
        }
        return null;
    }

}
