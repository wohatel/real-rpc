package com.github.wohatel.interaction.handler;

import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionReactionWaiter;
import com.github.wohatel.interaction.file.RpcSessionSignature;

/**
 * Interface for handling RPC session request messages.
 * Provides default implementations for session lifecycle events.
 */
public interface RpcSessionRequestMsgHandler {

    /**
     * Called when a new session starts.
     * Default implementation agrees to the session.
     *
     * @param contextWrapper The wrapper containing RPC session context information
     * @param waiter The reaction waiter for the session start event
     * @return The session signature, default is agreement
     */
    default RpcSessionSignature onSessionStart(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {
        return RpcSessionSignature.agree();
    }

    /**
     * Called when a request message is received.
     * Note that once the processing of a message is time-consuming,
     * it will affect the consumption of other messages,
     * so it is recommended to use asynchronous threads to process the read logic
     *
     * @param contextWrapper The wrapper containing RPC session context information
     * @param request The received RPC session request
     * @param waiter The reaction waiter for the request event
     */
    void onReceiveRequest(final RpcSessionContextWrapper contextWrapper, final RpcSessionRequest request, RpcSessionReactionWaiter waiter);


    /**
     * Default method called when a session stops. This method provides a default implementation
     * that can be overridden by implementing classes if needed.
     *
     * @param contextWrapper The context wrapper containing session-related information and metadata
     * @param waiter         The reaction waiter that handles the session's stopping reaction and callbacks
     */
    default void onSessionStop(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {

    }


    /**
     * Default implementation of the finally callback for RPC session handling.
     * This method is called after the execution of the RPC session, regardless of whether it completed successfully or threw an exception.
     *
     * @param contextWrapper The wrapper containing the RPC session context information
     * @param waiter         The waiter object used to handle the session reaction
     */
    default void onFinally(final RpcSessionContextWrapper contextWrapper, final RpcSessionReactionWaiter waiter) {

    }

}
