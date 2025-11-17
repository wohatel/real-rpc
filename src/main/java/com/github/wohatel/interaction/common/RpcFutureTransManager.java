package com.github.wohatel.interaction.common;


import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.util.DefaultVirtualThreadPool;
import com.github.wohatel.util.SessionManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for managing RPC future transactions.
 * This class is responsible for handling RPC sessions, requests, and reactions.
 * It uses a SessionManager to manage RPC futures with a default timeout.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcFutureTransManager {

    /**
     * SessionManager for managing RPC futures with a default timeout.
     * It uses a timeout handler for managing expired sessions.
     */
    @Getter
    private static final SessionManager<RpcFuture> RPC_FUTURE_SESSION_MANAGER = new SessionManager<>(RpcNumberConstant.OVER_TIME, RpcFutureTransManager::handleTimeOut);

    /**
     * Verifies and processes an RPC session request.
     *
     * @param rpcSessionRequest The RPC session request to verify
     * @return A RpcSessionFuture representing the session
     * @throws RpcException if the request is null, session is null, or session validation fails
     */
    public static RpcSessionFuture verifySessionRequest(RpcSessionRequest rpcSessionRequest) {
        if (rpcSessionRequest == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSessionRequest cannot be null");
        }
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        if (rpcSession == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "session the identity cannot be null");
        }
        if (contains(rpcSession.getSessionId())) {
            if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.TOSTART) {
                throw new RpcException(RpcErrorEnum.SEND_MSG, "sessions cannot be opened repeatedly");
            }
            RpcSessionFuture rpcFuture = getSessionFuture(rpcSession.getSessionId());
            RpcSessionProcess rpcSessionProcess = rpcFuture.getRpcSessionProcess();
            if (rpcSessionProcess != RpcSessionProcess.FINISHED) {
                rpcFuture.setRequestTime(System.currentTimeMillis());
                // 刷新时间
                RPC_FUTURE_SESSION_MANAGER.flushTime(rpcSession.getSessionId(), rpcSession.getTimeOutMillis());
            }
            return rpcFuture;
        } else {
            if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.RUNNING) {
                throw new RpcException(RpcErrorEnum.SEND_MSG, "session does not exist or has ended, and session messages cannot be sent");
            } else if (rpcSessionRequest.getSessionProcess() == RpcSessionProcess.FINISHED) {
                throw new RpcException(RpcErrorEnum.SEND_MSG, "the session does not exist or has ended, and there is no need to end the session");
            } else {
                RpcSessionFuture rpcFuture = new RpcSessionFuture(rpcSession.getTimeOutMillis());
                rpcFuture.setFutureId(rpcSession.getSessionId());
                rpcFuture.setRpcSessionProcess(RpcSessionProcess.TOSTART);
                RPC_FUTURE_SESSION_MANAGER.initSession(rpcSession.getSessionId(), rpcFuture, System.currentTimeMillis() + rpcSession.getTimeOutMillis());
                return rpcFuture;
            }
        }

    }

    /**
     * Retrieves an RPC session future by session ID from the RPC future session manager.
     * This method serves as a wrapper to cast the generic session object to an RpcSessionFuture.
     *
     * @param sessionId The unique identifier of the session to retrieve
     * @return RpcSessionFuture The session future associated with the given session ID
     * @throws ClassCastException If the session is not of type RpcSessionFuture
     */
    public static RpcSessionFuture getSessionFuture(String sessionId) {
        // Retrieve session from RPC future session manager and cast to RpcSessionFuture
        return (RpcSessionFuture) RPC_FUTURE_SESSION_MANAGER.getSession(sessionId);
    }

    /**
     * Adds a reaction to the RPC system and processes it based on the current state of the RPC session.
     * This method handles different states of the RPC session and performs appropriate actions.
     *
     * @param rpcReaction The reaction to be added and processed. Contains reaction details and ID.
     */
    public static void addReaction(RpcReaction rpcReaction) {
        // Early return if the reaction is null
        if (rpcReaction == null) {
            return;
        }
        // Early return if the reaction ID is null
        if (rpcReaction.getReactionId() == null) {
            return;
        }
        // Get the RPC future session associated with the reaction ID
        RpcFuture rpcFuture = RPC_FUTURE_SESSION_MANAGER.getSession(rpcReaction.getReactionId());
        // Early return if no session is found
        if (rpcFuture == null) {
            return;
        }
        // complete only call once
        rpcFuture.setReactionTime(System.currentTimeMillis());
        if (rpcFuture instanceof RpcSessionFuture rpcSessionFuture) {
            RpcSessionProcess rpcSessionProcess = rpcSessionFuture.getRpcSessionProcess();
            switch (rpcSessionProcess) {
                case TOSTART -> rpcFuture.complete(rpcReaction);

                case FINISHED -> remove(rpcReaction.getReactionId());

                case RUNNING -> {
                    RPC_FUTURE_SESSION_MANAGER.flushTime(rpcReaction.getReactionId(), rpcSessionFuture.getTimeOut());
                    executeOnReaction(rpcFuture, rpcReaction);
                }
            }
        } else {
            // 清掉
            rpcFuture.complete(rpcReaction);
            remove(rpcReaction.getReactionId());
            executeOnReaction(rpcFuture, rpcReaction);
        }
    }

    /**
     * Executes the given reaction on all listeners associated with the RPC future.
     * This method uses a virtual thread pool to handle the execution of each listener's reaction.
     *
     * @param rpcFuture   The RPC future containing the listeners to be notified
     * @param rpcReaction The reaction to be executed by each listener
     */
    private static void executeOnReaction(RpcFuture rpcFuture, RpcReaction rpcReaction) {
        // Get the list of listeners from the RPC future
        List<RpcReactionMsgListener> listeners = rpcFuture.getListeners();
        // Check if listeners exist before processing
        if (listeners != null) {
            // Iterate over a copy of the listeners list to avoid potential ConcurrentModificationException
            for (RpcReactionMsgListener rpcReactionMsgListener : new ArrayList<>(listeners)) {
                // Execute each listener's reaction in the virtual thread pool
                DefaultVirtualThreadPool.execute(() -> rpcReactionMsgListener.onReaction(rpcReaction));
            }
        }
    }

    /**
     * Adds a new RPC request to the system and creates a corresponding future object for tracking the response.
     * This method initializes a session for the request and sets a timeout for the operation.
     *
     * @param rpcRequest The RPC request object to be added, containing necessary request information
     * @param timeOut    The timeout duration for the request in milliseconds
     * @return RpcFuture The future object associated with this request, allowing for response tracking
     * @throws RpcException If the timeout configuration is not valid (timeOut <= 0)
     */
    public static RpcFuture addRequest(RpcRequest rpcRequest, long timeOut) {
        // Validate the request object is not null
        if (rpcRequest == null) {
            return null;
        }
        // Validate the request has a valid request ID
        if (rpcRequest.getRequestId() == null) {
            return null;
        }
        // Check if timeout is positive
        if (timeOut > 0) {
            // Create a new RpcFuture with the specified timeout
            RpcFuture rpcFuture = new RpcFuture(timeOut);
            // Set the future ID using the request ID
            rpcFuture.setFutureId(rpcRequest.getRequestId());
            RPC_FUTURE_SESSION_MANAGER.initSession(rpcRequest.getRequestId(), rpcFuture, System.currentTimeMillis() + timeOut);
            return rpcFuture;
        } else {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the timeout time configuration is not legal");
        }
    }

    /**
     * Removes and returns the RpcFuture associated with the given sessionId
     * from the RPC_FUTURE_SESSION_MANAGER.
     *
     * @param sessionId The unique identifier of the session to be removed
     * @return The RpcFuture object associated with the specified sessionId,
     * or null if no mapping exists for the sessionId
     */
    public static RpcFuture remove(String sessionId) {
        // Release the session by sessionId from the RPC_FUTURE_SESSION_MANAGER
        // and return the corresponding RpcFuture
        return RPC_FUTURE_SESSION_MANAGER.release(sessionId);
    }


    /**
     * This method returns the current number of concurrent sessions.
     * It utilizes the RPC_FUTURE_SESSION_MANAGER to retrieve the session count.
     *
     * @return The number of active sessions in the RPC future session manager
     */
    public static int concurrentSize() {
        // Return the size of the session from the RPC_FUTURE_SESSION_MANAGER
        return RPC_FUTURE_SESSION_MANAGER.sessionSize();
    }


    /**
     * Checks if a request with the given requestId exists in the RPC_FUTURE_SESSION_MANAGER.
     * This is a static method that provides a convenient way to verify the presence of a request.
     *
     * @param requestId The unique identifier of the request to be checked
     * @return true if the request exists in the RPC_FUTURE_SESSION_MANAGER, false otherwise
     */
    public static boolean contains(String requestId) {
        return RPC_FUTURE_SESSION_MANAGER.contains(requestId);
    }


    /**
     * Handles a timeout event for a given RPC session and future.
     * This method processes timeout events by checking the future's validity and executing timeout listeners.
     *
     * @param sessionId The ID of the RPC session that timed out
     * @param future    The RPC future associated with the timed out request
     */
    private static void handleTimeOut(String sessionId, RpcFuture future) {
        // If future is null, there's nothing to process, so return immediately
        if (future == null) {
            return;
        }
        // If it's a session future and the session process is already finished, return
        if (future instanceof RpcSessionFuture sessionFuture && sessionFuture.getRpcSessionProcess() == RpcSessionProcess.FINISHED) {
            return;
        }
        // Get all listeners associated with the future
        List<RpcReactionMsgListener> listeners = future.getListeners();
        // If there are listeners, execute each one in a new thread from the default virtual thread pool
        if (listeners != null) {
            for (RpcReactionMsgListener rpcReactionMsgListener : new ArrayList<>(listeners)) {
                DefaultVirtualThreadPool.execute(rpcReactionMsgListener::onTimeout);
            }
        }
    }

    /**
     * Flushes the time for a specific session
     *
     * @param sessionId   The unique identifier of the session
     * @param sessionTime The timestamp to be associated with the session
     */
    public static void flushTime(String sessionId, long sessionTime) {
        // Call the flushTime method of RPC_FUTURE_SESSION_MANAGER with provided parameters
        RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, sessionTime);
    }

    /**
     * Gracefully stops a session identified by the given session ID.
     * This method will mark the session as finished and update its last active time.
     *
     * @param sessionId The unique identifier of the session to be stopped
     * @return The RpcSessionFuture object associated with the session, or null if session doesn't exist
     */
    public static RpcSessionFuture stopSessionGracefully(String sessionId) {
        // Retrieve the session from the RPC future session manager
        RpcSessionFuture future = (RpcSessionFuture) RPC_FUTURE_SESSION_MANAGER.getSession(sessionId);
        // If session doesn't exist, return null
        if (future == null) {
            return null;
        }
        // Check if the session process is not already finished
        if (future.getRpcSessionProcess() != RpcSessionProcess.FINISHED) {
            // Mark the session process as finished
            future.setRpcSessionProcess(RpcSessionProcess.FINISHED);
            // Update the session's last active time with a timeout value
            RPC_FUTURE_SESSION_MANAGER.flushTime(sessionId, RpcNumberConstant.ONE_POINT_FILE_K);
        }
        // Return the modified session future
        return future;
    }
}
