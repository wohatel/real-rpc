package com.github.wohatel.initializer;

import com.github.wohatel.constant.RpcBaseAction;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.tcp.RpcDataReceiver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * description
 *
 * @author yaochuang 2025/09/16 09:30
 */
public class RpcMessageBaseInquiryHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcMsg rpcMsg = (RpcMsg) msg;
        if (rpcMsg.getRpcCommandType() == RpcCommandType.base) {
            RpcRequest request = rpcMsg.getPayload(RpcRequest.class);
            RpcBaseAction rpcBaseAction = RpcBaseAction.fromString(request.getRequestType());
            switch (rpcBaseAction) {
                case BASE_INQUIRY_SESSION -> {
                    String sessionId = request.getBody();
                    boolean running = RpcSessionTransManger.isRunning(sessionId);
                    RpcResponse response = request.toResponse();
                    response.setSuccess(running);
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }
                case BASE_INQUIRY_NODE_ID -> {
                    RpcResponse response = request.toResponse();
                    response.setBody(RpcDataReceiver.NODEID);
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }


}
