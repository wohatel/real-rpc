package com.github.wohatel.interaction.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;


/**
 * A class representing the signature for RPC file transfer operations.
 * This class provides methods to create instances for agreeing or rejecting file transfer requests.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcFileSignature {

    /**
     * Indicates whether the file transfer request is agreed or rejected.
     * True if the request is agreed, false if rejected.
     */
    private boolean agreed;

    /**
     * The file to be transferred.
     * This field is populated when the file transfer request is agreed.
     */
    private File file;

    /**
     * The transfer model for the file operation.
     * Specifies how the file should be transferred or rebuilt.
     */
    private RpcFileTransModel transModel;

    /**
     * Message containing details about the rejection reason.
     * This field is populated when the file transfer request is rejected.
     */
    private String msg;

    /**
     * Creates an instance of RpcFileSignature with agreement to transfer the specified file.
     *
     * @param file       The file to be transferred
     * @param transModel The transfer model to be used (if null, defaults to REBUILD)
     * @return A new RpcFileSignature instance with agreed status set to true
     */
    public static RpcFileSignature agree(File file, RpcFileTransModel transModel) {
        RpcFileSignature signature = new RpcFileSignature();
        signature.file = file;
        signature.transModel = transModel == null ? RpcFileTransModel.REBUILD : transModel;
        signature.agreed = true;
        return signature;
    }

    /**
     * Creates an instance of RpcFileSignature with rejection of the file transfer request.
     *
     * @param msg The reason for rejecting the file transfer request
     * @return A new RpcFileSignature instance with agreed status set to false and the provided message
     */
    public static RpcFileSignature reject(String msg) {
        RpcFileSignature signature = new RpcFileSignature();
        signature.agreed = false;
        signature.msg = msg;
        return signature;
    }
}
