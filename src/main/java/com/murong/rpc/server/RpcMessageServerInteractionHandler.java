package com.murong.rpc.server;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcSessionContext;
import com.murong.rpc.interaction.common.FileTransChannelDataManager;
import com.murong.rpc.interaction.common.RpcInteractionContainer;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * @author yaochuang
 */
@Setter
@Getter
@ChannelHandler.Sharable
@RequiredArgsConstructor
@Log
public class RpcMessageServerInteractionHandler extends ChannelInboundHandlerAdapter {
    private RpcFileRequestHandler rpcFileRequestHandler;
    private RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler;
    private RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMsg rpcMsg = (RpcMsg) msg;
        var type = rpcMsg.getRpcCommandType();

        switch (type) {
            case response -> {
                RpcResponse response = rpcMsg.getPayload(RpcResponse.class);
                RpcInteractionContainer.addResponse(response);
            }
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
                String sessionId = session.getSessionId();

                if (request.isSessionStart()) {
                    RpcSessionContext sessionContext = JSONObject.parseObject(request.getBody(), RpcSessionContext.class);
                    RpcSessionFuture future = RpcInteractionContainer.getSessionFuture(sessionId);
                    future.getContext().set(0, sessionContext);
                    rpcSessionRequestMsgHandler.sessionStart(ctx, session, sessionContext);
                } else if (request.isSessionRequest()) {
                    RpcSessionFuture future = RpcInteractionContainer.getSessionFuture(sessionId);
                    RpcSessionContext sessionContext = (RpcSessionContext) future.getContext().getFirst();
                    rpcSessionRequestMsgHandler.channelRead(ctx, session, sessionContext, request);
                } else if (request.isSessionFinish()) {
                    RpcInteractionContainer.remove(sessionId);
                    RpcSessionFuture future = RpcInteractionContainer.getSessionFuture(sessionId);
                    RpcSessionContext sessionContext = (RpcSessionContext) future.getContext().getFirst();
                    rpcSessionRequestMsgHandler.sessionStop(ctx, session, sessionContext);
                }
            }
            case file -> FileTransChannelDataManager.channelRead(ctx.channel(), rpcMsg, rpcFileRequestHandler);
            case heart -> RpcMsgTransUtil.sendHeart(ctx.channel());

            default -> {
            }
        }
    }

}
