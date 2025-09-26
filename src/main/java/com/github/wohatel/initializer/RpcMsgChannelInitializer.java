package com.github.wohatel.initializer;

import com.github.wohatel.decoder.RpcMsgDecoder;
import com.github.wohatel.decoder.RpcMsgEncoder;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
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

    private final RpcMessageInteractionHandler rpcMessageInteractionHandler = new RpcMessageInteractionHandler();

    private Consumer<SocketChannel> initChannelConsumer;

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMessageInteractionHandler.setRpcFileReceiverHandler(rpcFileReceiverHandler);
    }

    public void onRequestReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void onSessionRequestReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
    }

    /**
     * 添加的默认的编码解码和压缩器
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        if (initChannelConsumer != null) {
            initChannelConsumer.accept(socketChannel);
        } else {
            initChannel0(socketChannel);
        }
    }

    private void initChannel0(SocketChannel socketChannel) {
        // 默认最大的帧16M,如果接口超过16M说明是不合理的,需要将接口拆开,分成小数据
        int defaultMaxFrameLength = 16 * 1024 * 1024;
        socketChannel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(defaultMaxFrameLength, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("decoder", new RpcMsgDecoder());
        pipeline.addLast("encoder", new RpcMsgEncoder());
        pipeline.addLast("baseHandler", new RpcMessageBaseInquiryHandler());
        pipeline.addLast("msgHandler", rpcMessageInteractionHandler);
    }
}
