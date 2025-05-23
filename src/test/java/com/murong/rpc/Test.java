package com.murong.rpc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * description
 *
 * @author yaochuang 2025/03/25 17:41
 */
public class Test {


    public static void main(String[] args) throws IOException {
        for (int i = 0; i < 5000000; i++) {
            String fileName = "/Users/yaochuang/test/7ed752f71d471bd2e318ffcb4d3dd159_%s.swap";
            String format = String.format(fileName, i);
            System.out.println(format);
            Files.deleteIfExists(new File(format).toPath());
        }
    }
}
