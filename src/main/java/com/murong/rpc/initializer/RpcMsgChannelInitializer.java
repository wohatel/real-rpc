package com.murong.rpc.initializer;

import com.murong.rpc.decoder.RpcMsgCompressDecoder;
import com.murong.rpc.decoder.RpcMsgCompressEncoder;
import com.murong.rpc.decoder.RpcMsgDecoder;
import com.murong.rpc.decoder.RpcMsgEncoder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

/**
 * @author yaochuang
 */
@Data
@Accessors(chain = true)
public class RpcMsgChannelInitializer extends ChannelInitializer<SocketChannel> {

    protected Consumer<ChannelPipeline> initPipelineConsumer;

    public RpcMsgChannelInitializer() {
    }

    public RpcMsgChannelInitializer(Consumer<ChannelPipeline> initPipelineConsumer) {
        this.initPipelineConsumer = initPipelineConsumer;
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
        pipeline.addLast("decompress", new RpcMsgCompressDecoder());
        pipeline.addLast("compress", new RpcMsgCompressEncoder());
        pipeline.addLast("decoder", new RpcMsgDecoder());
        pipeline.addLast("encoder", new RpcMsgEncoder());
        if (initPipelineConsumer != null) {
            initPipelineConsumer.accept(pipeline);
        }
    }
}
