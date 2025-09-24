package com.github.wohatel.decoder;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.common.ByteBufPoolManager;
import com.github.wohatel.interaction.constant.RpcCommandType;
import com.github.wohatel.interaction.file.RpcFileRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author yaochuang
 */
@Slf4j
public class RpcMsgEncoder extends MessageToMessageEncoder<RpcMsg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMsg msg, List<Object> out) {
        /**
         * 第一个字节[],表是否压缩
         * 第二个字节: 消息体的类型
         * 接着四个字节: 消息的长度
         * 接下来-消息体
         * 接着四个字节: 文件的长度
         * 接下来-文件体
         */
        int flag = msg.isNeedCompress() ? 1 : 0;
        ByteBuf buffer = ctx.alloc().buffer(1024);
        // 写入第一个压缩标记位
        buffer.writeByte(flag);
        buffer.writeByte(msg.getRpcCommandType().getCode());
        // 写入消息体长度 (4字节)
        writePayload(buffer, msg.getPayload());
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
            if (fileBuf != null) {
                ByteBufPoolManager.release(rpcSession.getSessionId(), fileBuf);
            }
            if (rpcFileRequest.isFinished()) {
                ByteBufPoolManager.destory(rpcSession.getSessionId());
            }
        }
        out.add(buffer);
    }


    @SneakyThrows
    private void writePayload(ByteBuf buffer, Object payload) {
        // 先占位长度
        int lengthIndex = buffer.writerIndex();
        buffer.writeInt(0);

        int startIndex = buffer.writerIndex();
        try (ByteBufOutputStream outStream = new ByteBufOutputStream(buffer)) {
            JSON.writeTo(outStream, payload); // FastJSON2 直接写 UTF-8
            outStream.flush();
        }
        int endIndex = buffer.writerIndex();

        // 回写 payload 的长度
        buffer.setInt(lengthIndex, endIndex - startIndex);
    }

}