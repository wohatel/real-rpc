package com.github.wohatel.initializer;

import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcReaction;
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
            case reaction -> RpcFutureTransManager.addReaction(rpcMsg.getPayload(RpcReaction.class));
            case request -> {
                if (rpcSimpleRequestMsgHandler != null) {
                    RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
                    rpcSimpleRequestMsgHandler.channelRead(ctx, request);
                }
            }
            case session -> {
                RpcSessionRequest request = rpcMsg.getPayload(RpcSessionRequest.class);
                RpcSession session = request.getRpcSession();
                KeyValue<String, Boolean> keyValue = null;
                if (request.isSessionStart()) {
                    RpcReaction reaction = request.toReaction();
                    if (RpcSessionTransManger.isRunning(session.getSessionId())) {
                        String errorMsg = "{requestId:" + request.getRequestId() + "} build session id repeat";
                        keyValue = new KeyValue<>(errorMsg, false);
                    } else {
                        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
                        RpcSessionTransManger.initSession(context, session, ctx.channel().id().asShortText());
                        RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                        keyValue = RunnerUtil.execSilentException(() -> new KeyValue<>(null, rpcSessionRequestMsgHandler.sessionStart(ctx, contextWrapper)), e -> new KeyValue<>(e.getMessage(), false));
                    }
                    reaction.setSuccess(keyValue.getValue());
                    reaction.setMsg(keyValue.getKey());
                    RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                    if (!keyValue.getValue()) {
                        RpcSessionTransManger.release(session.getSessionId());
                    }
                } else if (request.isSessionRequest()) {
                    if (RpcSessionTransManger.isRunning(session.getSessionId())) {
                        RpcSessionTransManger.flush(session.getSessionId());
                        RpcSessionContextWrapper contextWrapper = RpcSessionTransManger.getContextWrapper(session.getSessionId());
                        rpcSessionRequestMsgHandler.channelRead(ctx, contextWrapper, request);
                    } else {
                        RpcReaction reaction = request.toReaction();
                        reaction.setMsg("{requestId:" + request.getRequestId() + "} the sending session message is abnormal and the session does not exist");
                        reaction.setSuccess(false);
                        RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
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
