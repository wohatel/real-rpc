package com.github.wohatel.initializer;

import com.github.wohatel.decoder.RpcMsgCompressDecoder;
import com.github.wohatel.decoder.RpcMsgCompressEncoder;
import com.github.wohatel.decoder.RpcMsgDecoder;
import com.github.wohatel.decoder.RpcMsgEncoder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yaochuang
 */
@Data
@Accessors(chain = true)
public class RpcMsgChannelInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * 初始化的
     */
    @Getter
    private List<Pair<String, ChannelHandler>> initChannelHandlers;

    public RpcMsgChannelInitializer() {
        init();
    }

    /**
     * 添加的默认的编码解码和压缩器
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
        ChannelPipeline pipeline = socketChannel.pipeline();
        initChannelHandlers.forEach(pair -> pipeline.addLast(pair.getLeft(), pair.getRight()));
    }

    private void init() {
        initChannelHandlers = new ArrayList<>();
        this.initChannelHandlers.add(Pair.of("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4)));
        this.initChannelHandlers.add(Pair.of("frameEncoder", new LengthFieldPrepender(4)));
        this.initChannelHandlers.add(Pair.of("decompress", new RpcMsgCompressDecoder()));
        this.initChannelHandlers.add(Pair.of("compress", new RpcMsgCompressEncoder()));
        this.initChannelHandlers.add(Pair.of("decoder", new RpcMsgDecoder()));
        this.initChannelHandlers.add(Pair.of("encoder", new RpcMsgEncoder()));
        this.initChannelHandlers.add(Pair.of("msgHandler", new RpcMessageInteractionHandler()));
    }
}
