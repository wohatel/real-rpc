package com.murong.rpc.initializer;

import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.FileTransChannelDataManager;
import com.murong.rpc.interaction.common.RpcInteractionContainer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.TransSessionManger0;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import com.murong.rpc.util.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * description
 *
 * @author yaochuang 2025/06/30 17:33
 */
@Setter
@Getter
@ChannelHandler.Sharable
@Log
@RequiredArgsConstructor
public class RpcMessageInteractionHandler extends ChannelInboundHandlerAdapter {

    private RpcFileRequestHandler rpcFileRequestHandler;
    private RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler;
    private RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler;
    /**
     * 针对ping的回应
     */
    private final boolean pong;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMsg rpcMsg = (RpcMsg) msg;
        switch (rpcMsg.getRpcCommandType()) {
            case response -> RpcInteractionContainer.addResponse(rpcMsg.getPayload(RpcResponse.class));

            case request -> {
                RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
                if (rpcSimpleRequestMsgHandler != null) {
                    rpcSimpleRequestMsgHandler.channelRead(ctx, request);
                } else if (request.isNeedResponse()) {
                    RpcMsgTransUtil.write(ctx.channel(), request.toResponse());
                }
            }

            case session -> {
                RpcSessionRequest request = rpcMsg.getPayload(RpcSessionRequest.class);
                RpcSession session = request.getRpcSession();
                if (request.isSessionStart()) {
                    if (TransSessionManger0.isRunning(session.getSessionId())) {
                        RpcResponse response = request.toResponse();
                        response.setMsg("{reqeustId:" + request.getRequestId() + "}构建session异常:会话id重复");
                        response.setSuccess(false);
                        RpcMsgTransUtil.write(ctx.channel(), response);
                    } else {
                        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
                        TransSessionManger0.initSession(session.getSessionId(), context, session);
                        rpcSessionRequestMsgHandler.sessionStart(ctx, session, context);
                    }
                } else if (request.isSessionRequest()) {
                    TransSessionManger0.flush(session.getSessionId());
                    RpcSessionContext context = TransSessionManger0.getSessionContext(session.getSessionId());
                    rpcSessionRequestMsgHandler.channelRead(ctx, session, request, context);
                } else if (request.isSessionFinish()) {
                    try {
                        boolean running = TransSessionManger0.isRunning(request.getRpcSession().getSessionId());
                        if (running) {
                            RpcSessionContext context = TransSessionManger0.getSessionContext(session.getSessionId());
                            rpcSessionRequestMsgHandler.sessionStop(ctx, session, context);
                        }
                    } finally {
                        TransSessionManger0.release(session.getSessionId());
                    }
                }
            }

            case file -> FileTransChannelDataManager.channelRead(ctx.channel(), rpcMsg, rpcFileRequestHandler);

            case heart -> this.handleHeart(ctx, msg);

            default -> {
            }
        }
    }

    private void handleHeart(ChannelHandlerContext ctx, Object msg) {
        if (pong) {
            RpcMsgTransUtil.sendHeart(ctx.channel());
        } else {
            ctx.fireChannelRead(msg);
        }
    }


}
