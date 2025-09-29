package com.github.wohatel.interaction.file;

import lombok.Getter;

import java.io.File;

/**
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
public class RpcFileLocal {

    private final File file;

    private final RpcFileTransModel transModel;

    public RpcFileLocal(File file, RpcFileTransModel transModel) {
        this.file = file;
        this.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
    }

    public RpcFileLocal(File file) {
        this(file, null);
    }

}
