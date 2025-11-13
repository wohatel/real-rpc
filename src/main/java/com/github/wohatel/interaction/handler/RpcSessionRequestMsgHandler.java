package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionReactionWaiter;
import com.github.wohatel.interaction.file.RpcSessionSignature;

public interface RpcSessionRequestMsgHandler {
    /**
     * Note that once the processing of a message is time-consuming,
     * it will affect the consumption of other messages,
     * so it is recommended to use asynchronous threads to process the read logic
     *
     */
    default RpcSessionSignature onSessionStart(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {
        return RpcSessionSignature.agree();
    }

    /**
     * Note that once the processing of a message is time-consuming,
     * it will affect the consumption of other messages,
     * so it is recommended to use asynchronous threads to process the read logic
     */
    void onReceiveRequest(final RpcSessionContextWrapper contextWrapper, final RpcSessionRequest request, RpcSessionReactionWaiter waiter);

    /**
     * client tell server: "session will be close",then server handle this service
     * only can exec once
     */
    default void onSessionStop(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {

    }

    /**
     * 最终执行
     *
     * @param contextWrapper contextWrapper
     */
    default void onFinally(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {

    }

}
