package com.github.wohatel.initializer;

import com.github.wohatel.decoder.RpcMsgBodyDecoder;
import com.github.wohatel.decoder.RpcMsgBodyEncoder;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

/**
 * @author yaochuang
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class RpcMsgChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Getter
    private final RpcMessageInteractionHandler rpcMessageInteractionHandler = new RpcMessageInteractionHandler();

    private Consumer<SocketChannel> initChannelConsumer;

    public void onFileReceive(RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcFileRequestMsgHandler(rpcFileRequestMsgHandler);
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

    /**
     * 初始化修正
     */
    public void initChannel(Consumer<SocketChannel> initChannelConsumer) {
        this.initChannelConsumer = initChannelConsumer;
    }

    private void initChannel0(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(RpcNumberConstant.DATA_LIMIT_M_16, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("decoder", new RpcMsgBodyDecoder());
        pipeline.addLast("encoder", new RpcMsgBodyEncoder());
        pipeline.addLast("msgHandler", rpcMessageInteractionHandler);
    }
}
