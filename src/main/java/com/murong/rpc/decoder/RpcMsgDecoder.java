package com.murong.rpc.decoder;

import com.alibaba.fastjson2.JSON;
import com.murong.rpc.interaction.constant.RpcCommandType;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * @author yaochuang
 */
public class RpcMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int readableBytes = in.readableBytes();
        if (readableBytes < 8) {
            in.resetReaderIndex();
            return; // 不够读长度字段
        }
        RpcMsg msg = new RpcMsg();
        byte isCompress = in.readByte();
        msg.setNeedCompress(isCompress == 1);
        byte type = in.readByte(); // **读取消息类型**
        int msgLength = in.readInt(); // **读取消息体长度**
        byte[] jsonBytes = new byte[msgLength];
        in.readBytes(jsonBytes); // **读取消息体**
        msg.setRpcCommandType(RpcCommandType.fromCode(type));
        if (msg.getRpcCommandType() == RpcCommandType.request) {
            msg.setPayload(JSON.parseObject(jsonBytes, RpcRequest.class));
        } else if (msg.getRpcCommandType() == RpcCommandType.session) {
            msg.setPayload(JSON.parseObject(jsonBytes, RpcSessionRequest.class));
        } else if (msg.getRpcCommandType() == RpcCommandType.response) {
            msg.setPayload(JSON.parseObject(jsonBytes, RpcResponse.class));
        } else if (msg.getRpcCommandType() == RpcCommandType.heart) {
            msg.setPayload(null);
        } else if (msg.getRpcCommandType() == RpcCommandType.file) {
            RpcFileRequest rpcFileRequest = JSON.parseObject(jsonBytes, RpcFileRequest.class);
            msg.setPayload(rpcFileRequest);
            int fileLength = in.readInt(); // **读取文件长度**
            if (fileLength > 0) {
                ByteBuf fileBuf = in.readRetainedSlice(fileLength); // **使用 `readRetainedSlice` 避免数据复制**
                msg.setByteBuffer(fileBuf);
            }
        }
        out.add(msg); // **解析完成，加入解码结果**
    }
}