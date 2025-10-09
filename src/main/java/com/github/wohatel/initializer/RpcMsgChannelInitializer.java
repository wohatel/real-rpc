package com.github.wohatel.initializer;

import com.github.wohatel.decoder.RpcMsgBodyDecoder;
import com.github.wohatel.decoder.RpcMsgBodyEncoder;
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

    private int maxFramePayloadLength = 16 * 1024 * 1024;

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

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        if (initChannelConsumer != null) {
            initChannelConsumer.accept(socketChannel);
        } else {
            initChannel0(socketChannel);
        }
    }

    private void initChannel0(SocketChannel socketChannel) {
        int defaultMaxFrameLength = maxFramePayloadLength;
        socketChannel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(defaultMaxFrameLength, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("decoder", new RpcMsgBodyDecoder());
        pipeline.addLast("encoder", new RpcMsgBodyEncoder());
        pipeline.addLast("baseHandler", new RpcMessageBaseInquiryHandler());
        pipeline.addLast("msgHandler", rpcMessageInteractionHandler);
    }
}
