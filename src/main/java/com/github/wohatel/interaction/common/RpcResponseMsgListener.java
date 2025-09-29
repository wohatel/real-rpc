package com.github.wohatel.interaction.common;

import com.github.wohatel.interaction.base.RpcResponse;

public interface RpcResponseMsgListener {

    void onResponse(RpcResponse response);

    default void onTimeout() {
    }

    /**
     * it will be called when client stopSession or interruptSession
     */
    default void onSessionInterrupt() {
    }
}