package com.murong.rpc.interaction.handler;

import com.murong.rpc.interaction.base.RpcResponse;

/**
 * 监听消息响应
 */
public interface RpcResponseMsgListener {
    /**
     * 收到响应
     *
     * @param response
     */
    void onResponse(RpcResponse response);

    /**
     * 超时
     */
    default void onTimeout() {
    }
}