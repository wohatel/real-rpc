package com.github.wohatel.interaction.file;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
@Slf4j
public class RpcFileSignatureRotary {

    private RpcFileSignature signature;

    public RpcFileSignatureRotaryResult rotary(long remoteFileSize) {
        File file = signature.getFile();
        RpcFileTransModel transModel = signature.getTransModel();
        if (file == null) {
            return RpcFileSignatureRotaryResult.fail("file config exception->file is null");
        }
        if (file.exists() && file.isDirectory()) {
            return RpcFileSignatureRotaryResult.fail("file config exception->file is directory");
        }
        try {
            Path path = file.toPath();
            return switch (transModel) {
                case REBUILD -> {
                    // If it exists, it will be deleted, and in the end it will be rebuilt
                    if (file.exists()) {
                        Files.deleteIfExists(path);
                    }
                    Files.createDirectories(path.getParent());
                    Files.createFile(path);
                    RpcFileSignatureRotaryResult result = RpcFileSignatureRotaryResult.success(0);
                    result.needTrans = remoteFileSize != 0;
                    yield result;
                }
                case APPEND -> {
                    // Prioritize creating a catalog
                    Files.createDirectories(path.getParent());
                    // If it exists, it will be continued, and if it does not exist, it will be rebuilt
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    RpcFileSignatureRotaryResult result = RpcFileSignatureRotaryResult.success(0);
                    result.needTrans = remoteFileSize != 0;
                    yield result;
                }
                case RESUME -> {
                    // Prioritize creating a catalog
                    Files.createDirectories(path.getParent());
                    // If it exists, it will be continued, and if it does not exist, it will be rebuilt
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    if (remoteFileSize < file.length()) {
                        yield RpcFileSignatureRotaryResult.fail("the file length of receive is bigger or eq than sender");
                    } else {
                        RpcFileSignatureRotaryResult result = RpcFileSignatureRotaryResult.success(Files.size(path));
                        result.needTrans = remoteFileSize > file.length();
                        yield result;
                    }
                }
                case SKIP -> {
                    if (!file.exists()) {
                        Files.createDirectories(path.getParent());
                        Files.createFile(path);
                        RpcFileSignatureRotaryResult result = RpcFileSignatureRotaryResult.success(0L);
                        result.needTrans = remoteFileSize != 0;
                        yield result;
                    } else {
                        yield RpcFileSignatureRotaryResult.success(0L);
                    }
                }
            };
        } catch (Exception e) {
            log.error("rotary rpcFileSignature exception:", e);
            return RpcFileSignatureRotaryResult.fail("rotary RpcFileSignatureRotaryResult:" + e.getMessage());
        }

    }

    public static RpcFileSignatureRotary fromLocalWrapper(RpcFileSignature signature) {
        RpcFileSignatureRotary rpcFileSignatureRotary = new RpcFileSignatureRotary();
        rpcFileSignatureRotary.signature = signature;
        return rpcFileSignatureRotary;
    }

    @Data
    public static class RpcFileSignatureRotaryResult {
        private boolean success;
        private String msg;
        private long writeIndex;
        private boolean needTrans;

        public static RpcFileSignatureRotaryResult fail(String msg) {
            RpcFileSignatureRotaryResult result = new RpcFileSignatureRotaryResult();
            result.success = false;
            result.msg = msg;
            return result;
        }

        public static RpcFileSignatureRotaryResult success(long writeIndex) {
            RpcFileSignatureRotaryResult result = new RpcFileSignatureRotaryResult();
            result.success = true;
            result.writeIndex = writeIndex;
            return result;
        }
    }
}
