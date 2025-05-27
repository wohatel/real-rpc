package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.common.FileTransSessionManger;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcResponse;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RpcFileTransInterrupter {

    private final Channel channel;
    private final String sessionId;

    public synchronized void interrupt() {
        if (FileTransSessionManger.isNormal(sessionId)) {
            FileTransSessionManger.release(sessionId);
            RpcResponse response = new RpcResponse();
            response.setRequestId(sessionId);
            response.setSuccess(false);
            response.setMsg("接收端终止");
            RpcMsgTransUtil.write(channel, response);
        }
    }

}
