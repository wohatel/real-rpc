package com.github.wohatel.interaction.file;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A utility class for handling file signature rotation operations.
 * Provides methods to manage file transfers based on different transmission models.
 */
@Getter
@Slf4j
public class RpcFileSignatureRotary {

    private RpcFileSignature signature; // The file signature containing file and transfer information

    /**
     * Processes file rotation based on the transmission model.
     *
     * @param remoteFileSize The size of the remote file
     * @return RpcFileSignatureRotaryResult containing the operation result and transfer status
     */
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

    /**
     * Creates a new RpcFileSignatureRotary instance from a local file signature wrapper.
     *
     * @param signature The file signature to wrap
     * @return A new RpcFileSignatureRotary instance
     */
    public static RpcFileSignatureRotary fromLocalWrapper(RpcFileSignature signature) {
        RpcFileSignatureRotary rpcFileSignatureRotary = new RpcFileSignatureRotary();
        rpcFileSignatureRotary.signature = signature;
        return rpcFileSignatureRotary;
    }

    /**
     * Result class for file signature rotation operations.
     * Contains operation status, message, write index, and transfer requirements.
     */
    @Data
    public static class RpcFileSignatureRotaryResult {
        private boolean success; // Operation success status
        private String msg; // Message describing the result or error
        private long writeIndex; // Current write position in the file
        private boolean needTrans; // Whether file transfer is needed

        /**
         * Creates a failed result with an error message.
         *
         * @param msg The error message
         * @ RpcFileSignatureRotaryResult with success status set to false
         */
        public static RpcFileSignatureRotaryResult fail(String msg) {
            RpcFileSignatureRotaryResult result = new RpcFileSignatureRotaryResult();
            result.success = false;
            result.msg = msg;
            return result;
        }

        /**
         * Creates a successful result with a write index.
         *
         * @param writeIndex The write index to set
         * @return RpcFileSignatureRotaryResult with success status set to true
         */
        public static RpcFileSignatureRotaryResult success(long writeIndex) {
            RpcFileSignatureRotaryResult result = new RpcFileSignatureRotaryResult();
            result.success = true;
            result.writeIndex = writeIndex;
            return result;
        }
    }
}
