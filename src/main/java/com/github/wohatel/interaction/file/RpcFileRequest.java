package com.github.wohatel.interaction.file;

import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * Represents a request for file transfer in RPC communication.
 * This class extends RpcSessionRequest and provides additional properties
 * for file transfer operations such as block information and serialization.
 */
@Getter
@Setter
@ToString(callSuper = true)
@Accessors(chain = true)
public class RpcFileRequest extends RpcSessionRequest {
    private boolean lastBlock;
    private long blockSize;
    private long serial;

    public RpcFileRequest(RpcSession rpcSession) {
        super(rpcSession);
    }
}
