package com.github.wohatel.interaction.file;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
public class RpcFileWrapperUtil {

    private final File file;

    private final RpcFileTransModel transModel;

    private long writeIndex;

    private String msg;

    private boolean needTrans;

    public RpcFileWrapperUtil(File file, RpcFileTransModel transModel) {
        this.file = file;
        this.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
    }

    public void init(long remoteFileSize) {
        if (file == null) {
            this.msg = "file config exception->file is null";
            return;
        }
        if (file.exists() && file.isDirectory()) {
            this.msg = "file config exception->file is directory";
            return;
        }
        try {
            Path path = file.toPath();
            switch (this.transModel) {
                case REBUILD -> {
                    // If it exists, it will be deleted, and in the end it will be rebuilt
                    if (file.exists()) {
                        Files.deleteIfExists(path);
                    }
                    Files.createDirectories(path.getParent());
                    Files.createFile(path);
                    this.writeIndex = 0L;
                    this.needTrans = remoteFileSize != 0;
                }
                case APPEND -> {
                    // Prioritize creating a catalog
                    Files.createDirectories(path.getParent());
                    // If it exists, it will be continued, and if it does not exist, it will be rebuilt
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    this.writeIndex = 0L;
                    this.needTrans = remoteFileSize != 0;
                }
                case RESUME -> {
                    // Prioritize creating a catalog
                    Files.createDirectories(path.getParent());
                    // If it exists, it will be continued, and if it does not exist, it will be rebuilt
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    this.writeIndex = Files.size(path);
                    this.needTrans = remoteFileSize > file.length();
                    if (remoteFileSize < file.length()) {
                        this.msg = "the file length of receive is bigger or eq than sender";
                    }
                }
                case SKIP -> {
                    if (!file.exists()) {
                        Files.createDirectories(path.getParent());
                        Files.createFile(path);
                        this.needTrans = remoteFileSize != 0;
                    }
                    this.writeIndex = 0L;
                }
                default -> {

                }
            }
        } catch (Exception e) {
            this.needTrans = false;
            this.msg = "unknow:" + e.getMessage();
            this.writeIndex = 0L;
        }

    }

    public boolean isInterruptByInit() {
        return !this.needTrans && StringUtils.isBlank(this.msg);
    }

    public static RpcFileWrapperUtil fromLocalWrapper(RpcFileLocal localWrapper) {
        return new RpcFileWrapperUtil(localWrapper.getFile(), localWrapper.getTransModel());
    }
}
