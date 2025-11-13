package com.github.wohatel.interaction.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcFileSignature {

    private boolean agreed;

    private File file;

    private RpcFileTransModel transModel;

    private String msg;

    public static RpcFileSignature agree(File file, RpcFileTransModel transModel) {
        RpcFileSignature signature = new RpcFileSignature();
        signature.file = file;
        signature.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
        signature.agreed = true;
        return signature;
    }

    public static RpcFileSignature reject(String msg) {
        RpcFileSignature signature = new RpcFileSignature();
        signature.agreed = false;
        signature.msg = msg;
        return signature;
    }
}
