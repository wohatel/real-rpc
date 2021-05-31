package com.murong.rpc.util;

import org.bouncycastle.util.Arrays;

import java.nio.ByteBuffer;

public class ArrayUtil {

    public static byte[] toBytes(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        byte[] array = byteBuffer.array();
        int limit = byteBuffer.limit();
        return clone(array, limit);
    }

    public static byte[] clone(byte[] bytes, int len) {
        byte[] newBytes = new byte[len];
        System.arraycopy(bytes, 0, newBytes, 0, len);
        return newBytes;
    }

    public static byte[] clone(byte[] bytes) {
        return Arrays.clone(bytes);
    }


    public static void main(String[] args) {
        String abc = "bbaaccddeeff";
        byte[] bytes = abc.getBytes();

        System.out.println(bytes.length);
        byte[] clone = clone(bytes, 3);
        System.out.println(new String(clone));
    }

}
