package com.github.wohatel.initializer;

import com.github.wohatel.decoder.RpcMsgCompressDecoder;
import com.github.wohatel.decoder.RpcMsgCompressEncoder;
import com.github.wohatel.decoder.RpcMsgDecoder;
import com.github.wohatel.decoder.RpcMsgEncoder;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.util.LinkedNode;
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

/**
 * @author yaochuang
 */
@Data
@Accessors(chain = true)
public class RpcMsgChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final RpcMessageInteractionHandler rpcMessageInteractionHandler = new RpcMessageInteractionHandler();

    /**
     * 初始化的
     */
    @Getter
    private LinkedNode<String, ChannelHandler> initChannelHandlers;

    public RpcMsgChannelInitializer() {
        init();
    }

    public void onFileReceive(RpcFileReceiverHandler rpcFileReceiverHandler) {
        rpcMessageInteractionHandler.setRpcFileReceiverHandler(rpcFileReceiverHandler);
    }

    public void onMsgReceive(RpcSimpleRequestMsgHandler rpcSimpleRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSimpleRequestMsgHandler(rpcSimpleRequestMsgHandler);
    }

    public void onSessionMsgReceive(RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler) {
        rpcMessageInteractionHandler.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
    }

    /**
     * 添加的默认的编码解码和压缩器
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.config().setAllocator(PooledByteBufAllocator.DEFAULT);
        ChannelPipeline pipeline = socketChannel.pipeline();
        initChannelHandlers.forEach(pair -> {
            if (pair.getKey() != null) {
                pipeline.addLast(pair.getKey(), pair.getValue());
            }
        });
    }

    private void init() {
        this.initChannelHandlers = LinkedNode.build("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
        this.initChannelHandlers.addLast(LinkedNode.build("frameEncoder", new LengthFieldPrepender(4)));
        this.initChannelHandlers.addLast(LinkedNode.build("decompress", new RpcMsgCompressDecoder()));
        this.initChannelHandlers.addLast(LinkedNode.build("compress", new RpcMsgCompressEncoder()));
        this.initChannelHandlers.addLast(LinkedNode.build("decoder", new RpcMsgDecoder()));
        this.initChannelHandlers.addLast(LinkedNode.build("encoder", new RpcMsgEncoder()));
        this.initChannelHandlers.addLast(LinkedNode.build("msgHandler", rpcMessageInteractionHandler));
    }
}
