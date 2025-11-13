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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


/**
 * @author yaochuang
 */
@Slf4j
public class RpcSessionChannelDataTransProxy {

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
        if (request.getSessionProcess() == RpcSessionProcess.TOSTART) {
            handleToStart(ctx, request, rpcSessionRequestMsgHandler);
        } else if (request.getSessionProcess() == RpcSessionProcess.RUNNING) {
            handleRunning(ctx, request, rpcSessionRequestMsgHandler);
        } else if (request.getSessionProcess() == RpcSessionProcess.FINISHED) {
            handleFinished(ctx, request, rpcSessionRequestMsgHandler);
        }
    }


    public static void handleToStart(ChannelHandlerContext ctx, RpcSessionRequest request, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        RpcSession session = request.getRpcSession();
        RpcReaction reaction = request.toReaction();
        if (RpcSessionTransManger.isRunning(session.getSessionId())) {
            String errorMsg = "{requestId:" + request.getRequestId() + "} build session id repeat";
            reaction.setMsg(errorMsg);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setSuccess(false);
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            return;
        }
        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
        RpcSessionTransManger.initSession(context, session, ctx);
        RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
        RpcSessionReactionWaiter waiter = RpcSessionTransManger.getWaiter(session.getSessionId());
        RpcSessionSignature signature = RunnerUtil.execSilentException(() -> rpcSessionRequestMsgHandler.onSessionStart(contextWrapper, waiter), e -> RpcSessionSignature.reject(e.getMessage()));
        reaction.setSuccess(signature.isAgreed());
        reaction.setMsg(signature.getMsg());
        RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        if (!signature.isAgreed()) {
            RpcSessionTransManger.release(session.getSessionId());
        } else {
            // 注册最终release事件
            RpcSessionTransManger.registOnRelease(session.getSessionId(), t -> RpcSessionRequestMsgHandlerExecProxy.onFinally(rpcSessionRequestMsgHandler, contextWrapper, waiter));
        }
    }

    public static void handleRunning(ChannelHandlerContext ctx, RpcSessionRequest request, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        RpcSession session = request.getRpcSession();
        if (RpcSessionTransManger.isRunning(session.getSessionId())) {
            RpcSessionTransManger.flush(session.getSessionId());
            RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
            RpcSessionReactionWaiter waiter = RpcSessionTransManger.getWaiter(session.getSessionId());
            rpcSessionRequestMsgHandler.onReceiveRequest(contextWrapper, request, waiter);
        } else {
            RpcReaction reaction = request.toReaction();
            reaction.setMsg("{requestId:" + request.getRequestId() + "} the sending session message is abnormal and the session does not exist");
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.SESSION_LOSE.getCode());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    public static void handleFinished(ChannelHandlerContext ctx, RpcSessionRequest request, RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        RpcSession session = request.getRpcSession();
        try {
            if (RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId())) {
                RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                RpcSessionReactionWaiter waiter = RpcSessionTransManger.getWaiter(session.getSessionId());
                RpcSessionRequestMsgHandlerExecProxy.sessionStop(rpcSessionRequestMsgHandler, contextWrapper, waiter);
            }
        } finally {
            RpcSessionTransManger.release(session.getSessionId());
        }
    }

}