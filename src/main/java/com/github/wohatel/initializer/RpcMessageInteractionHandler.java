package com.github.wohatel.initializer;

import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcFileChannelDataTransManager;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionContextWrapper;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.KeyValue;
import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 *
 * @author yaochuang 2025/06/30 17:33
 */
@Setter
@Getter
@ChannelHandler.Sharable
@Slf4j
@RequiredArgsConstructor
public class RpcMessageInteractionHandler extends ChannelInboundHandlerAdapter {
    private RpcFileReceiverHandler rpcFileReceiverHandler;
    private RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler;
    private RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMsg rpcMsg = (RpcMsg) msg;
        switch (rpcMsg.getRpcCommandType()) {
            case response -> RpcFutureTransManager.addResponse(rpcMsg.getPayload(RpcResponse.class));
            case request -> {
                if (rpcSimpleRequestMsgHandler != null) {
                    RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
                    rpcSimpleRequestMsgHandler.channelRead(ctx, request);
                }
            }
            case session -> {
                RpcSessionRequest request = rpcMsg.getPayload(RpcSessionRequest.class);
                RpcSession session = request.getRpcSession();
                KeyValue<String, Boolean> linkedNode = null;
                if (request.isSessionStart()) {
                    RpcResponse response = request.toResponse();
                    if (RpcSessionTransManger.isRunning(session.getSessionId())) {
                        String errorMsg = "{requestId:" + request.getRequestId() + "}build session id repeat";
                        linkedNode = new KeyValue<>(errorMsg, false);
                    } else {
                        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
                        RpcSessionTransManger.initSession(context, session, ctx.channel().id().asShortText());
                        RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                        linkedNode = RunnerUtil.execSilentException(() -> new KeyValue<>(null, rpcSessionRequestMsgHandler.sessionStart(ctx, contextWrapper)), e -> new KeyValue<>(e.getMessage(), false));
                    }
                    response.setSuccess(linkedNode.getValue());
                    response.setMsg(linkedNode.getKey());
                    RpcMsgTransManager.sendResponse(ctx.channel(), response);
                    if (!linkedNode.getValue()) {
                        RpcSessionTransManger.release(session.getSessionId());
                    }
                } else if (request.isSessionRequest()) {
                    if (RpcSessionTransManger.isRunning(session.getSessionId())) {
                        RpcSessionTransManger.flush(session.getSessionId());
                        RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                        rpcSessionRequestMsgHandler.channelRead(ctx, contextWrapper, request);
                    } else {
                        RpcResponse response = request.toResponse();
                        response.setMsg("{requestId:" + request.getRequestId() + "} the sending session message is abnormal and the session does not exist");
                        response.setSuccess(false);
                        RpcMsgTransManager.sendResponse(ctx.channel(), response);
                    }
                } else if (request.isSessionFinish()) {
                    try {
                        if (RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId())) {
                            RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                            rpcSessionRequestMsgHandler.sessionStop(ctx, contextWrapper);
                        }
                    } finally {
                        RpcSessionTransManger.release(session.getSessionId());
                    }
                }
            }

            case file -> RpcFileChannelDataTransManager.channelRead(ctx, rpcMsg, rpcFileReceiverHandler);

            default -> {
            }
        }
    }
}
