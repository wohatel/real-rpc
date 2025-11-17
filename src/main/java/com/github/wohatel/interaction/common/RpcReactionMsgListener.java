package com.github.wohatel.interaction.common;

import com.github.wohatel.interaction.base.RpcReaction;

/**
 * A functional interface for handling RPC reaction messages.
 * This interface ensures that implementing classes provide a specific behavior
 * when a reaction message is received.
 *
 * @since 1.0
 */
@FunctionalInterface
public interface RpcReactionMsgListener {

    /**
     * Called when a reaction message is received.
     * This method should implement the logic for handling the reaction.
     *
     * @param reaction The RPC reaction object containing reaction details
     */
    void onReaction(RpcReaction reaction);

    /**
     * Default method for handling timeout scenarios.
     * This method can be overridden by implementing classes if needed.
     * By default, it does nothing.
     */
    default void onTimeout() {
    }
}