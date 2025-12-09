package com.github.wohatel.interaction.common;


import com.github.wohatel.interaction.base.RpcReaction;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * RpcSessionReactionWaiter class handles RPC session-specific reactions and manages session state.
 * It extends RpcReactionWaiter to provide session-aware reaction handling capabilities.
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcSessionReactionWaiter extends RpcReactionWaiter {

    @Getter
    private final String sessionId;  // Unique identifier for the RPC session

    /**
     * Constructs a new RpcSessionReactionWaiter with the given context and session ID.
     *
     * @param ctx       The channel handler context for network communication
     * @param sessionId The unique identifier for this RPC session
     */
    public RpcSessionReactionWaiter(ChannelHandlerContext ctx, String sessionId) {
        super(ctx);
        this.sessionId = sessionId;
    }

    /**
     * Sends an RPC reaction only if it matches the current session ID and the session is active.
     *
     * @param reaction The RPC reaction to be sent
     */
    @Override
    public void sendReaction(RpcReaction reaction) {
        if (reaction == null) {
            return;
        }
        if (!this.sessionId.equals(reaction.getReactionId())) {
            return;
        }
        if (RpcSessionTransManger.isRunning(sessionId)) {
            super.sendReaction(reaction);
        }
    }

    /**
     * Forces the interruption of an active session by releasing its resources.
     * This method is typically called when an immediate termination of a session is required.
     *
     */
    public void forceInterruptSession() {
        // Release the session resources through the RPC session transfer manager
        RpcSessionTransManger.release(sessionId);
    }

    /**
     * Executes a runnable when the current session is released.
     * This method delegates the execution to RpcSessionTransManger by passing the session ID and the runnable.
     *
     * @param runnable The runnable to be executed upon session release
     */
    public void onSessionRelease(Runnable runnable) {
        // Delegate to RpcSessionTransManger to handle the release callback
        RpcSessionTransManger.onRelease(sessionId, runnable);
    }

    /**
     * Returns the Runnable object responsible for session release operations.
     * This method retrieves the release event associated with the current session ID.
     *
     * @return Runnable A Runnable object that represents the session release event,
     * or null if no release event is associated with the session ID.
     */
    public Runnable getSessionRelease() {
        return RpcSessionTransManger.getOnReleaseEvent(sessionId);
    }
}
