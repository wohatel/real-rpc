package com.github.wohatel.interaction.common;

import com.github.wohatel.interaction.base.RpcReaction;

public interface RpcReactionMsgListener {

    void onReaction(RpcReaction reaction);

    default void onTimeout() {
    }
}