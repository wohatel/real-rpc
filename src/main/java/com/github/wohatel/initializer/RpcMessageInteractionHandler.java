package com.github.wohatel.initializer;

import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.FileTransChannelDataManager;
import com.github.wohatel.interaction.common.RpcInteractionContainer;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.TransSessionManger;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.util.JsonUtil;
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
            case response -> RpcInteractionContainer.addResponse(rpcMsg.getPayload(RpcResponse.class));

            case request -> {
                RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
                if (rpcSimpleRequestMsgHandler != null) {
                    rpcSimpleRequestMsgHandler.channelRead(ctx, request);
                }
            }
            
            case session -> {
                RpcSessionRequest request = rpcMsg.getPayload(RpcSessionRequest.class);
                RpcSession session = request.getRpcSession();
                if (request.isSessionStart()) {
                    if (TransSessionManger.isRunning(session.getSessionId())) {
                        RpcResponse response = request.toResponse();
                        response.setMsg("{reqeustId:" + request.getRequestId() + "}构建session异常:会话id重复");
                        response.setSuccess(false);
                        RpcMsgTransUtil.write(ctx.channel(), response);
                    } else {
                        RpcSessionContext context = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
                        TransSessionManger.initSession(session.getSessionId(), context, session);
                        rpcSessionRequestMsgHandler.sessionStart(ctx, session, context);
                    }
                } else if (request.isSessionRequest()) {
                    TransSessionManger.flush(session.getSessionId());
                    RpcSessionContext context = TransSessionManger.getSessionContext(session.getSessionId());
                    rpcSessionRequestMsgHandler.channelRead(ctx, session, request, context);
                } else if (request.isSessionFinish()) {
                    try {
                        boolean running = TransSessionManger.isRunning(request.getRpcSession().getSessionId());
                        if (running) {
                            RpcSessionContext context = TransSessionManger.getSessionContext(session.getSessionId());
                            rpcSessionRequestMsgHandler.sessionStop(ctx, session, context);
                        }
                    } finally {
                        TransSessionManger.release(session.getSessionId());
                    }
                }
            }

            case file -> FileTransChannelDataManager.channelRead(ctx, rpcMsg, rpcFileReceiverHandler);

            default -> {
            }
        }
    }
}
