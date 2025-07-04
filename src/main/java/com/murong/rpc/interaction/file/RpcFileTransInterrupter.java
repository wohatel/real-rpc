package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.common.TransSessionManger0;
import io.netty.channel.Channel;
import org.apache.commons.lang3.tuple.Triple;

/**
 * @author yaochuang
 */
public class RpcFileTransInterrupter {

    public static void interrupt(String sessionId) {
        if (TransSessionManger0.isRunning(sessionId)) {
            Triple<RpcFileContext, RpcFileLocalWrapper, Channel> data = TransSessionManger0.getFileData(sessionId);
            RpcResponse response = new RpcResponse();
            response.setRequestId(sessionId);
            response.setSuccess(false);
            response.setMsg("接收端终止");
            RpcMsgTransUtil.write(data.getRight(), response);
            TransSessionManger0.release(sessionId);
        }
    }

    public static void interrupt(RpcSession rpcSession) {
        interrupt(rpcSession.getSessionId());
    }

}
