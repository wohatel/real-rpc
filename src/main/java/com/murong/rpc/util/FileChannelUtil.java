package com.murong.rpc.util;

import com.murong.rpc.interaction.RpcFileRequest;
import com.murong.rpc.interaction.RpcMsgTransUtil;
import com.murong.rpc.interaction.RpcResponse;
import io.netty.channel.Channel;

import java.io.IOException;

public class FileChannelUtil {

    public static void readFileRequest(Channel channel, RpcFileRequest rpcFileRequest) throws IOException {
        FileUtil.appendFile("/Users/yaochuang/test/abc.txt", rpcFileRequest.getBytes(), rpcFileRequest.getPosition());
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        RpcMsgTransUtil.write(channel, rpcResponse);
    }
}
