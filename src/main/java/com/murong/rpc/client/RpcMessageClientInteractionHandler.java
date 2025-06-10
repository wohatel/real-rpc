package com.murong.rpc.client;

import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
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
                    rpcSessionRequestMsgHandler.sessionStart(ctx, rpcSession);
                } else if (request.isSessionRequest()) {
                    rpcSessionRequestMsgHandler.channelRead(ctx, rpcSession, request);
                } else if (request.isSessionFinish()) {
                    rpcSessionRequestMsgHandler.sessionStop(ctx, rpcSession);
                }
            }

            case file -> FileTransChannelDataManager.channelRead(ctx.channel(), rpcMsg, rpcFileRequestHandler);

            case heart -> ctx.fireChannelRead(msg);

            default -> {
            }
        }
    }


}
