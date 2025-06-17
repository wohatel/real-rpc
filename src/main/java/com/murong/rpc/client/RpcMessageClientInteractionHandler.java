package com.murong.rpc.client;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.FileTransChannelDataManager;
import com.murong.rpc.interaction.common.RpcInteractionContainer;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.RpcSessionManager;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.interaction.handler.RpcSimpleRequestMsgHandler;
import com.murong.rpc.util.JsonUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

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
                RpcSession session = request.getRpcSession();
                if (request.isSessionStart()) {
                    RpcSessionContext rpcSessionContext = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
                    RpcSessionManager.init(session.getSessionId(), rpcSessionContext, session.getTimeOutMillis());
                    rpcSessionRequestMsgHandler.sessionStart(ctx, session);
                } else if (request.isSessionRequest()) {
                    RpcSessionManager.flush(session.getSessionId(), session.getTimeOutMillis());
                    rpcSessionRequestMsgHandler.channelRead(ctx, session, request);
                } else if (request.isSessionFinish()) {
                    try {
                        rpcSessionRequestMsgHandler.sessionStop(ctx, session);
                    } finally {
                        RpcSessionManager.release(session.getSessionId());
                    }
                }
            }

            case file -> FileTransChannelDataManager.channelRead(ctx.channel(), rpcMsg, rpcFileRequestHandler);

            case heart -> ctx.fireChannelRead(msg);

            default -> {
            }
        }
    }


}
