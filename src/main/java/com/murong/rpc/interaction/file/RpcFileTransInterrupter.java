package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.FileTransSessionManger;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcResponse;
import io.netty.channel.Channel;
import org.apache.commons.lang3.tuple.Triple;

/**
 * @author yaochuang
 */
public class RpcFileTransInterrupter {

    public static void interrupt(String sessionId) {
        if (FileTransSessionManger.isRunning(sessionId)) {
            Triple<RpcFileContext, RpcFileWrapper, Channel> data = FileTransSessionManger.getData(sessionId);
            RpcResponse response = new RpcResponse();
            response.setRequestId(sessionId);
            response.setSuccess(false);
            response.setMsg("接收端终止");
            RpcMsgTransUtil.write(data.getRight(), response);
            FileTransSessionManger.release(sessionId);
        }
    }

    public static void interrupt(RpcSession rpcSession) {
        interrupt(rpcSession.getSessionId());
    }

}
