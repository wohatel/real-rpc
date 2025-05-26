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

    public void init() throws IOException {
        if (file == null) {
            throw new RuntimeException("文件配置异常file->空指针");
        }
        if (file.exists() && file.isDirectory()) {
            throw new RuntimeException(String.format("接收文件配置异常file->%s:模式下以目录形式存在", this.transModel.name()));
        }
        Path path = file.toPath();
        switch (this.transModel) {
            case CREATNEW -> {
                // 存在就抛异常,不存在就创建
                if (file.exists()) {
                    throw new RuntimeException("接收文件文件配置异常file->CREATNEW:模式下文件已存在,请检查");
                } else {
                    Files.createDirectories(path.getParent());
                    Files.createFile(path);
                    this.writeIndex = 0L;
                }
            }
            case REBUILD -> {
                // 存在就删除,最后都是重建
                if (file.exists()) {
                    Files.deleteIfExists(path);
                }
                Files.createDirectories(path.getParent());
                Files.createFile(path);
                this.writeIndex = 0L;
            }
            case APPEND -> {
                // 优先创建目录
                Files.createDirectories(path.getParent());
                // 存在就续传,不存在就重建
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                this.writeIndex = 0L;
            }
            case RESUME -> {
                // 优先创建目录
                Files.createDirectories(path.getParent());
                // 存在就续传,不存在就重建
                if (!Files.exists(path)) {
                    Files.createFile(path);
                }
                this.writeIndex = Files.size(path);
            }
            default -> {
            }
        }
    }

    public void verify(long remoteFileLength) throws IOException {
        // 只有在续传的时候需要检查文件大小
        if (RpcFileTransModel.RESUME == this.transModel) {
            if (remoteFileLength < this.writeIndex) {
                throw new RuntimeException("文件传输失败: 传输一端文件长度小于本地文件长度, 无需续传");
            }
        }
    }
}
