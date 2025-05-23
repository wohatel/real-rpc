package com.murong.rpc.initializer;

import com.murong.rpc.decoder.RpcMsgCompressDecoder;
import com.murong.rpc.decoder.RpcMsgCompressEncoder;
import com.murong.rpc.decoder.RpcMsgDecoder;
import com.murong.rpc.decoder.RpcMsgEncoder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yaochuang
 */
public class RpcMsgChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Getter
    private final List<ChannelHandler> channelHandlerList = new ArrayList<>();

    public RpcMsgChannelInitializer(ChannelHandler... channelHandlers) {
        if (ArrayUtils.isNotEmpty(channelHandlers)) {
            this.channelHandlerList.addAll(Arrays.asList(channelHandlers));
        }
    }

    public RpcMsgChannelInitializer addLastHandler(ChannelHandler channelHandler) {
        this.channelHandlerList.add(channelHandler);
        return this;
    }


    /**
     * 添加的默认的编码解码和压缩器
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
        ChannelPipeline pipeline = socketChannel.pipeline();

        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("decompress", new RpcMsgCompressEncoder());
        pipeline.addLast("compress", new RpcMsgCompressDecoder());
        pipeline.addLast("decoder", new RpcMsgDecoder());
        pipeline.addLast("encoder", new RpcMsgEncoder());

        if (!channelHandlerList.isEmpty()) {
            for (ChannelHandler channelHandler : channelHandlerList) {
                pipeline.addLast(channelHandler);
            }
        }
    }
}
