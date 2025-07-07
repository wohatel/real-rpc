package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.TransSessionManger;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author yaochuang
 */
public class RpcFileTransInterrupter {

    public static void interrupt(ChannelHandlerContext ctx, String sessionId) {
        if (TransSessionManger.isRunning(sessionId)) {
            RpcResponse response = new RpcResponse();
            response.setRequestId(sessionId);
            response.setSuccess(false);
            response.setMsg("接收端终止");
            RpcMsgTransUtil.write(ctx.channel(), response);
            TransSessionManger.release(sessionId);
        }
    }

    public static void interrupt(ChannelHandlerContext ctx, RpcSession rpcSession) {
        interrupt(ctx, rpcSession.getSessionId());
    }

}
