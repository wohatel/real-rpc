package com.github.wohatel.interaction.file;

import lombok.Getter;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
public class RpcFileLocal {
    /**
     * 文件位置
     */
    private final File file;
    /**
     * 文件是否追加或续传
     */
    private final RpcFileTransModel transModel;

    /**
     * @param file 目标文件存储
     */
    public RpcFileLocal(File file, RpcFileTransModel transModel) {
        this.file = file;
        this.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
    }

    /**
     * @param file 目标文件存储
     */
    public RpcFileLocal(File file) {
        this(file, null);
    }

}
