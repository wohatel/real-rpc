package com.murong.rpc.interaction.file;

import lombok.Getter;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
public class RpcFileLocalWrapper {
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
    public RpcFileLocalWrapper(File file, RpcFileTransModel transModel) {
        this.file = file;
        this.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
    }

    /**
     * @param file 目标文件存储
     */
    public RpcFileLocalWrapper(File file) {
        this(file, null);
    }

}
