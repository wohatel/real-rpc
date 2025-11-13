package com.github.wohatel.interaction.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 *
 * @author yaochuang 2025/05/13 14:41
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSessionSignature {

    private boolean agreed;

    private String msg;

    public static RpcSessionSignature agree() {
        RpcSessionSignature signature = new RpcSessionSignature();
        signature.agreed = true;
        return signature;
    }

    public static RpcSessionSignature reject(String msg) {
        RpcSessionSignature signature = new RpcSessionSignature();
        signature.agreed = false;
        signature.msg = msg;
        return signature;
    }
}
