package com.murong.rpc.interaction.file;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
public class RpcFileWrapper {

    /**
     * 文件位置
     */
    private final File file;
    /**
     * 文件是否追加或续传
     */
    private final RpcFileTransModel transModel;
    /**
     * 传输开始的索引
     */
    private long writeIndex;

    /**
     * 错误信息
     */
    private String msg;

    /**
     * 是否需要传输
     */
    private boolean needTrans;

    /**
     * @param file 目标文件存储
     */
    public RpcFileWrapper(File file, RpcFileTransModel transModel) {
        this.file = file;
        this.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
    }

    /**
     * @param file 目标文件存储
     */
    public RpcFileWrapper(File file) {
        this(file, null);
    }

    public void init(long remoteFileSize) {

        if (file == null) {
            this.msg = "文件配置异常file->文件为null";
            return;
        }
        if (file.exists() && file.isDirectory()) {
            this.msg = "接收文件配置异常:文件下以目录形式存在";
            return;
        }
        try {
            Path path = file.toPath();
            switch (this.transModel) {
                case REBUILD -> {
                    // 存在就删除,最后都是重建
                    if (file.exists()) {
                        Files.deleteIfExists(path);
                    }
                    Files.createDirectories(path.getParent());
                    Files.createFile(path);
                    this.writeIndex = 0L;
                    this.needTrans = remoteFileSize != 0;
                }
                case APPEND -> {
                    // 优先创建目录
                    Files.createDirectories(path.getParent());
                    // 存在就续传,不存在就重建
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    this.writeIndex = 0L;
                    this.needTrans = remoteFileSize != 0;
                }
                case RESUME -> {
                    // 优先创建目录
                    Files.createDirectories(path.getParent());
                    // 存在就续传,不存在就重建
                    if (!Files.exists(path)) {
                        Files.createFile(path);
                    }
                    this.writeIndex = Files.size(path);
                    this.needTrans = remoteFileSize > file.length();
                    if (remoteFileSize < file.length()) {
                        this.msg = "接收方文件占用内存不小于发送方,无需续传";
                    }
                }
                case SKIP -> {
                    if (!file.exists()) {
                        Files.createDirectories(path.getParent());
                        Files.createFile(path);
                        this.needTrans = true;
                    }
                    this.writeIndex = 0L;
                }
                default -> {

                }
            }
        } catch (Exception e) {
            this.needTrans = false;
            this.msg = e.getMessage();
            this.writeIndex = 0L;
        }

    }
}
