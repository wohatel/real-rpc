package com.github.wohatel.interaction.base;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class RpcReaction extends RpcRelay {
    private String reactionId;
    private String origin;
    private boolean success = true;
    private String command;
    private String msg;
    private int code;

    public static RpcReaction fromRequest(RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            throw new IllegalArgumentException("RpcRequest is null");
        }
        if (rpcRequest instanceof RpcSessionRequest sessionRequest) {
            RpcReaction reaction = new RpcReaction();
            reaction.setReactionId(sessionRequest.getRpcSession().getSessionId());
            reaction.setOrigin(sessionRequest.getRequestId());
            return reaction;
        }
        RpcReaction reaction = new RpcReaction();
        reaction.setReactionId(rpcRequest.getRequestId());
        reaction.setOrigin(rpcRequest.getRequestId());
        return reaction;
    }

    public static RpcReaction fromSession(RpcSession rpcSession) {
        RpcReaction reaction = new RpcReaction();
        reaction.setReactionId(rpcSession.getSessionId());
        reaction.setOrigin(rpcSession.getSessionId());
        return reaction;
    }
}
