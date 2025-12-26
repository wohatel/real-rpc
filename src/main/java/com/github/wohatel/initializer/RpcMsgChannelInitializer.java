package com.github.wohatel.initializer;

import com.github.wohatel.decoder.RpcMsgBodyDecoder;
import com.github.wohatel.decoder.RpcMsgBodyEncoder;
import com.github.wohatel.interaction.common.RpcHeartHandler;
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

/**
 * @author yaochuang
 */
@EqualsAndHashCode(callSuper = true)  // Generates equals and hashCode methods including superclass fields
@Accessors(chain = true)  // Enables method chaining for setters
public class RpcMsgChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Getter  // Auto-generates getter for this field
    private final RpcMessageInteractionHandler rpcMessageInteractionHandler = new RpcMessageInteractionHandler();

    private RpcHeartHandler rpcHeartHandler;

    public RpcMsgChannelInitializer(RpcHeartHandler rpcHeartHandler) {
        this.rpcHeartHandler = rpcHeartHandler;
    }

    public RpcMsgChannelInitializer() {

    }

    /**
     * Sets the file request message handler
     *
     * @param rpcFileRequestMsgHandler Handler for file request messages
     */
    public void onFileReceive(RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcFileRequestMsgHandler(rpcFileRequestMsgHandler);
    }

    /**
     * Sets the simple request message handler
     *
     * @param rpcSimpleRequestMsgHandler Handler for simple request messages
     */
    public void onRequestReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    /**
     * Sets the session request message handler
     *
     * @param rpcSessionRequestMsgHandler Handler for session request messages
     */
    public void onSessionRequestReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
    }

    /**
     * Initializes the channel pipeline
     *
     * @param socketChannel The socket channel to initialize
     * @throws Exception If initialization fails
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(RpcNumberConstant.DATA_LIMIT_M_16, 0, 4, 0, 4));
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
        pipeline.addLast("decoder", new RpcMsgBodyDecoder());
        pipeline.addLast("encoder", new RpcMsgBodyEncoder());
        if (rpcHeartHandler != null) {
//            pipeline.addLast("idleStateHandler", rpcHeartHandler.toIdleStateHandler()).addLast("heartHandler", rpcHeartHandler);
            pipeline.addLast("idleStateHandler", rpcHeartHandler);
        }
        pipeline.addLast("msgHandler", rpcMessageInteractionHandler);
    }
}
