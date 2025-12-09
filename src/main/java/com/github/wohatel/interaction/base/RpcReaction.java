package com.github.wohatel.interaction.base;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A class representing a reaction in RPC communication.
 * Extends RpcRelay and provides functionality for handling reaction responses.
 * Uses Lombok annotations for boilerplate code generation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(chain = true)
public class RpcReaction extends RpcRelay {
    private String reactionId;  // Unique identifier for the reaction
    private String origin;     // Origin of the reaction
    private boolean success = true;  // Default success status is true
    private String command;     // Command associated with the reaction
    private String msg;        // Message content of the reaction
    private int code;          // Status code of the reaction

    /**
     * Creates a RpcReaction from an RpcRequest.
     * Handles both RpcSessionRequest and regular RpcRequest instances.
     *
     * @param rpcRequest The request to create a reaction from
     * @return A new RpcReaction instance
     * @throws IllegalArgumentException if the rpcRequest is null
     */
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

    /**
     * Creates a RpcReaction from an RpcSession.
     *
     * @param rpcSession The session to create a reaction from
     * @return A new RpcReaction instance
     */
    public static RpcReaction fromSession(RpcSession rpcSession) {
        return fromSession(rpcSession.getSessionId());
    }

    /**
     * Creates a RpcReaction from an RpcSession.
     *
     * @param sessionId The session to create a reaction from
     * @return A new RpcReaction instance
     */
    public static RpcReaction fromSession(String sessionId) {
        RpcReaction reaction = new RpcReaction();
        reaction.setReactionId(sessionId);
        reaction.setOrigin(sessionId);
        return reaction;
    }
}
