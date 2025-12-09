package com.github.wohatel.interaction.proxy;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionReactionWaiter;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.file.RpcSessionSignature;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;



/**
 * RpcSessionChannelDataTransProxy is a proxy class that handles RPC session data transmission.
 * It processes incoming messages through different session states and delegates to appropriate handlers.
 * This class is designed to be a singleton with private constructor access.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RpcSessionChannelDataTransProxy {

    /**
     * Handles channel read events for RPC messages.
     * Processes the message based on the current session state.
     *
     * @param ctx                         The ChannelHandlerContext for the channel
     * @param rpcMsg                      The received RPC message
     * @param rpcSessionRequestMsgHandler The handler for session requests
     * @throws Exception if there's an error during processing
     */
    @SneakyThrows
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        RpcSessionRequest request = rpcMsg.getPayload(RpcSessionRequest.class);
        // 校验session是否开启了handler
        if (rpcSessionRequestMsgHandler == null) {
            RpcReaction reaction = request.toReaction();
            String errorMsg = "remote endpoint has no session handler";
            reaction.setMsg(errorMsg);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setSuccess(false);
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            return;
        }
        RpcSessionProcess sessionProcess = request.getSessionProcess();
        switch (sessionProcess) {
            case TOSTART -> handleToStart(ctx, request, rpcSessionRequestMsgHandler);
            case RUNNING -> handleRunning(ctx, request, rpcSessionRequestMsgHandler);
            case FINISHED -> handleFinished(request, rpcSessionRequestMsgHandler);
        }
    }

    /**
     * Handles the start of an RPC session by processing a session request and managing session state.
     *
     * @param ctx                         The ChannelHandlerContext for the network channel
     * @param request                     The RpcSessionRequest containing session initialization data
     * @param rpcSessionRequestMsgHandler The handler for processing session-related messages
     */
    public static void handleToStart(ChannelHandlerContext ctx, RpcSessionRequest request, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        // Extract the RPC session from the request
        RpcSession session = request.getRpcSession();
        // Convert the request to a reaction object for response
        RpcReaction reaction = request.toReaction();
        // Check if a session with this ID is already running
        if (RpcSessionTransManger.isRunning(session.getSessionId())) {
            // If session exists, create error message and send rejection
            String errorMsg = "{requestId:" + request.getRequestId() + "} build session id repeat";
            reaction.setMsg(errorMsg);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setSuccess(false);
            RunnerUtil.execSilent(() -> RpcMsgTransManager.sendReaction(ctx.channel(), reaction));
            return;
        }
        // Deserialize the request body to get session context
        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
        // Create a wrapper for the session context
        RpcSessionContextWrapper contextWrapper = new RpcSessionContextWrapper(session, context);
        // Create a waiter for handling session reaction
        RpcSessionReactionWaiter waiter = new RpcSessionReactionWaiter(ctx, session.getSessionId());
        // signature is not be null
        RpcSessionSignature signature = RunnerUtil.execSilentNullOrException(() -> rpcSessionRequestMsgHandler.signature(contextWrapper, waiter), () -> RpcSessionSignature.reject("remote session signature error: signature is null"), e -> RpcSessionSignature.reject(e.getMessage()));
        reaction.setSuccess(signature.isAgreed());
        reaction.setMsg(signature.getMsg());
        RunnerUtil.execSilent(() -> RpcMsgTransManager.sendReaction(ctx.channel(), reaction));
        if (signature.isAgreed()) {
            // 注册最终release事件
            RpcSessionTransManger.initSession(context, session, ctx);
            RpcSessionTransManger.onRelease(session.getSessionId(), () -> RpcSessionRequestMsgHandlerExecProxy.onFinally(rpcSessionRequestMsgHandler, contextWrapper, waiter));
        } else {
            RpcSessionRequestMsgHandlerExecProxy.onFinally(rpcSessionRequestMsgHandler, contextWrapper, waiter);
        }


    }

    /**
     * Handles a running RPC session request by checking if the session is active and processing it accordingly.
     * If the session is running, it flushes the session, retrieves the context wrapper and waiter, and processes the request.
     * If the session is not running, it creates a failed reaction indicating the session is lost and sends it back.
     *
     * @param ctx                         The ChannelHandlerContext which contains the channel information
     * @param request                     The RpcSessionRequest containing the session and request details
     * @param rpcSessionRequestMsgHandler The handler for processing RPC session requests
     */
    public static void handleRunning(ChannelHandlerContext ctx, RpcSessionRequest request, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        // Get the RPC session from the request
        RpcSession session = request.getRpcSession();
        // Check if the session with the given ID is currently running
        if (RpcSessionTransManger.isRunning(session.getSessionId())) {
            // If running, flush the session to ensure all messages are processed
            RpcSessionTransManger.flush(session.getSessionId());
            // Get the context wrapper associated with the session
            RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
            // Get the waiter associated with the session
            RpcSessionReactionWaiter waiter = RpcSessionTransManger.getWaiter(session.getSessionId());
            // Process the request using the provided handler
            rpcSessionRequestMsgHandler.onReceiveRequest(contextWrapper, request, waiter);
        } else {
            // If session is not running, create a failed reaction
            RpcReaction reaction = request.toReaction();
            // Set error message with the request ID
            reaction.setMsg("{requestId:" + request.getRequestId() + "} the sending session message is abnormal and the session does not exist");
            // Mark the reaction as failed
            reaction.setSuccess(false);
            // Set the error code for session loss
            reaction.setCode(RpcErrorEnum.SESSION_LOSE.getCode());
            // Send the failed reaction back through the channel
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    /**
     * Handles the completion of a RPC session request.
     * This method processes the finished RPC session by checking if it's still running,
     * executing necessary cleanup operations, and ensuring proper resource release.
     *
     * @param request                     The RpcSessionRequest containing session information
     * @param rpcSessionRequestMsgHandler The handler for RPC session request messages
     */
    public static void handleFinished(RpcSessionRequest request, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        // Get the RPC session from the request
        RpcSession session = request.getRpcSession();
        try {
            // Check if the session with the given ID is currently running
            if (RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId())) {
                // Get the context wrapper for the session
                RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                // Get the waiter associated with the session
                RpcSessionReactionWaiter waiter = RpcSessionTransManger.getWaiter(session.getSessionId());
                // Execute session stop operations through the proxy
                RpcSessionRequestMsgHandlerExecProxy.sessionStop(rpcSessionRequestMsgHandler, contextWrapper, waiter);
            }
        } finally {
            // Ensure the session resources are released regardless of exceptions
            RpcSessionTransManger.release(session.getSessionId());
        }
    }

}