package com.github.wohatel.interaction.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * A class representing the signature of an RPC (Remote Procedure Call) session.
 * It provides methods to create instances that indicate agreement or rejection of the session.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSessionSignature {

    /**
     * A boolean flag indicating whether the session agreement has been reached.
     * True if agreed, false otherwise.
     */
    private boolean agreed;

    /**
     * A message providing additional information, typically used when the session is rejected.
     */
    private String msg;

    /**
     * Creates a new RpcSessionSignature instance indicating agreement to the session.
     * The agreed flag is set to true, and the message is set to null.
     *
     * @return A new RpcSessionSignature instance with agreed set to true.
     */
    public static RpcSessionSignature agree() {
        RpcSessionSignature signature = new RpcSessionSignature();
        signature.agreed = true;
        return signature;
    }

    /**
     * Creates a new RpcSessionSignature instance indicating rejection of the session.
     * The agreed flag is set to false, and the message is set to the provided reason.
     *
     * @param msg The reason for rejecting the session.
     * @return A new RpcSessionSignature instance with agreed set to false and the provided message.
     */
    public static RpcSessionSignature reject(String msg) {
        RpcSessionSignature signature = new RpcSessionSignature();
        signature.agreed = false;
        signature.msg = msg;
        return signature;
    }
}
