package com.murong.rpc.client;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionContext;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.FileTransChannelDataManager;
import com.murong.rpc.interaction.common.RpcInteractionContainer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

/**
 * @author yaochuang
 */
@Setter
@Getter
@ChannelHandler.Sharable
@Log
public class RpcMessageClientInteractionHandler extends ChannelInboundHandlerAdapter {
    private RpcFileRequestHandler rpcFileRequestHandler;
    private RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler;
    private RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler;

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
                RpcSession rpcSession = request.getRpcSession();
                if (request.isSessionStart()) {
                    RpcSessionContext sessionContext = JSONObject.parseObject(request.getBody(), RpcSessionContext.class);
                    RpcSessionFuture sessionFuture = RpcInteractionContainer.getSessionFuture(rpcSession.getSessionId());
                    sessionFuture.getContext().set(0, sessionContext);
                    rpcSessionRequestMsgHandler.sessionStart(ctx, rpcSession, sessionContext);
                } else if (request.isSessionRequest()) {
                    RpcSessionFuture sessionFuture = RpcInteractionContainer.getSessionFuture(rpcSession.getSessionId());
                    RpcSessionContext sessionContext = (RpcSessionContext) sessionFuture.getContext().getFirst();
                    rpcSessionRequestMsgHandler.channelRead(ctx, rpcSession, sessionContext, request);
                } else if (request.isSessionFinish()) {
                    RpcSessionFuture rpcSessionFuture = RpcInteractionContainer.stopSessionGracefully(rpcSession.getSessionId());
                    RpcSessionContext sessionContext = (RpcSessionContext) rpcSessionFuture.getContext().getFirst();
                    rpcSessionRequestMsgHandler.sessionStop(ctx, rpcSession, sessionContext);
                }
            }

            case file -> FileTransChannelDataManager.channelRead(ctx.channel(), rpcMsg, rpcFileRequestHandler);

            case heart -> ctx.fireChannelRead(msg);

            default -> {
            }
        }
    }


}
