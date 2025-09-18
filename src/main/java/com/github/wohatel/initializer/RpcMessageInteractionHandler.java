package com.github.wohatel.initializer;

import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcFileChannelDataTransManager;
import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.LinkedNode;
import com.github.wohatel.util.RunnerUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


/**
 * description
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
                LinkedNode<String, Boolean> linkedNode = null;
                if (request.isSessionStart()) {
                    RpcResponse response = request.toResponse();
                    if (RpcSessionTransManger.isRunning(session.getSessionId())) {
                        String errorMsg = "{reqeustId:" + request.getRequestId() + "}构建session异常:会话id重复";
                        linkedNode = LinkedNode.build(errorMsg, false);
                    } else {
                        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
                        RpcSessionTransManger.initSession(context, session, ctx.channel().id().asShortText());
                        linkedNode = RunnerUtil.execSilentException(() -> LinkedNode.build(null, rpcSessionRequestMsgHandler.sessionStart(ctx, session, context)), e -> LinkedNode.build(e.getMessage(), false));
                    }
                    response.setSuccess(linkedNode.getValue());
                    response.setMsg(linkedNode.getKey());
                    RpcMsgTransUtil.write(ctx.channel(), response);
                    if (!linkedNode.getValue()) {
                        RpcSessionTransManger.release(session.getSessionId());
                    }
                } else if (request.isSessionRequest()) {
                    if (RpcSessionTransManger.isRunning(session.getSessionId())) {
                        RpcSessionTransManger.flush(session.getSessionId());
                        RpcSessionContext context = RpcSessionTransManger.getSessionContext(session.getSessionId());
                        rpcSessionRequestMsgHandler.channelRead(ctx, session, request, context);
                    } else {
                        RpcResponse response = request.toResponse();
                        response.setMsg("{reqeustId:" + request.getRequestId() + "}发送会话消息异常,会话不存在");
                        response.setSuccess(false);
                        RpcMsgTransUtil.write(ctx.channel(), response);
                    }
                } else if (request.isSessionFinish()) {
                    try {
                        if (RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId())) {
                            RpcSessionContext context = RpcSessionTransManger.getSessionContext(session.getSessionId());
                            rpcSessionRequestMsgHandler.sessionStop(ctx, session, context);
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
