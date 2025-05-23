package com.murong.rpc.decoder;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.common.ByteBufPoolManager;
import com.murong.rpc.interaction.constant.RpcCommandType;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class RpcMsgEncoder extends MessageToMessageEncoder<RpcMsg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, List<Object> out) {
        /**
         * 前两个字节[][]相同,表是否压缩
         * 第一个字节: 消息体的类型
         * 接着四个字节: 消息的长度
         * 接下来-消息体
         * 接着四个字节: 文件的长度
         * 接下来-文件体
         */
        int flag = msg.isNeedCompress() ? 1 : 0;
        ByteBuf buffer = ctx.alloc().buffer();
        // 写入第一个压缩标记位
        buffer.writeByte(flag);
        // 写入第二个压缩标记位(不可少)
        buffer.writeByte(flag);
        buffer.writeByte(msg.getRpcCommandType().getCode());
        // 写入消息体长度 (4字节)
        String jsonString = JSONObject.toJSONString(msg.getPayload());
        byte[] bytes = jsonString.getBytes(StandardCharsets.UTF_8);
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
        if (msg.getRpcCommandType() == RpcCommandType.file) {
            RpcFileRequest rpcFileRequest = msg.getPayload(RpcFileRequest.class);
            RpcSession rpcSession = rpcFileRequest.getRpcSession();
            // 接下来处理文件
            ByteBuf fileBuf = msg.getByteBuffer();
            if (fileBuf != null) {
                buffer.writeInt(fileBuf.readableBytes());
                buffer.writeBytes(fileBuf);
            } else {
                buffer.writeInt(0);
            }
            ByteBufPoolManager.release(rpcSession.getSessionId(), fileBuf);
        }
        out.add(buffer);
    }


}