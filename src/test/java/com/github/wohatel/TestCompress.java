package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author yaochuang 2025/09/18 17:27
 */
public class TestCompress {

    private static RpcServer server;
    private static RpcDefaultClient client;
    private static NioEventLoopGroup group;

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        // 线程组暂时用一个
        group = new NioEventLoopGroup();
        server = new RpcServer(8765, group, group);
        // 等待服务端开启成功
        server.start().sync();
        client = new RpcDefaultClient("127.0.0.1", 8765, group);
        // 等待客户端连接成功
        client.connect().sync();
    }

    /**
     * 客户端发送消息
     *
     */
    @Test
    void clientSendMsg() throws InterruptedException {
        // 绑定服务端接收消息处理
        server.onRequestReceive((ctx, req) -> {
            // 打印消息体
            String body = req.getBody();
            System.out.println("获取到消息体" + body);
        });
        Thread.sleep(1000);
        // 客户度发送消息
        String body = """
                package com.github.wohatel.decoder;
                
                import com.alibaba.fastjson2.JSON;
                import com.github.wohatel.interaction.base.RpcMsg;
                import com.github.wohatel.interaction.base.RpcRequest;
                import com.github.wohatel.interaction.base.RpcResponse;
                import com.github.wohatel.interaction.base.RpcSessionRequest;
                import com.github.wohatel.interaction.constant.RpcCommandType;
                import com.github.wohatel.interaction.file.RpcFileRequest;
                import io.netty.buffer.ByteBuf;
                import io.netty.buffer.CompositeByteBuf;
                import io.netty.channel.ChannelHandlerContext;
                import io.netty.channel.embedded.EmbeddedChannel;
                import io.netty.handler.codec.MessageToMessageDecoder;
                import io.netty.handler.codec.compression.Lz4FrameDecoder;
                import io.netty.util.ReferenceCountUtil;
                
                import java.util.List;
                
                /**
                 * @author yaochuang
                 */
                public class RpcMsgDecoder extends MessageToMessageDecoder<ByteBuf> {
                    private final ThreadLocal<EmbeddedChannel> decompressChannel = ThreadLocal.withInitial(() -> new EmbeddedChannel(new Lz4FrameDecoder()));
                
                    @Override
                    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                        ByteBuf byteBuf = null;
                        try {
                            byteBuf = tryDecompress(in);
                            RpcMsg rpcMsg = decodeMsg(byteBuf);
                            out.add(rpcMsg);
                        } finally {
                            ReferenceCountUtil.safeRelease(byteBuf);
                        }
                    }
                
                    /**
                     * 解码消息体到 RpcMsg
                     * 1: 是否压缩
                     * 2: 消息类型
                     * 3: 消息长度
                     * 4: 消息体
                     * 5: 文件长度
                     * 6: 文件
                     */
                    private RpcMsg decodeMsg(ByteBuf input) {
                        RpcMsg msg = new RpcMsg();
                        boolean compressed = input.readBoolean();
                        msg.setNeedCompress(compressed);
                        int type = input.readInt();
                        msg.setRpcCommandType(RpcCommandType.fromCode(type));
                        int payloadLength = input.readInt();
                        byte[] payloadBytes = new byte[payloadLength];
                        input.readBytes(payloadBytes);
                        int fileLength = input.readInt();
                        switch (msg.getRpcCommandType()) {
                            case request, base -> msg.setPayload(JSON.parseObject(payloadBytes, RpcRequest.class));
                            case session -> msg.setPayload(JSON.parseObject(payloadBytes, RpcSessionRequest.class));
                            case response -> msg.setPayload(JSON.parseObject(payloadBytes, RpcResponse.class));
                            case file -> {
                                RpcFileRequest fileRequest = JSON.parseObject(payloadBytes, RpcFileRequest.class);
                                msg.setPayload(fileRequest);
                                if (fileLength > 0) {
                                    ByteBuf fileBuf = input.readRetainedSlice(fileLength);
                                    msg.setByteBuffer(fileBuf); // 下游负责 release
                                }
                            }
                        }
                        return msg;
                    }
                
                
                    /**
                     * 尝试解压
                     */
                    private ByteBuf tryDecompress(ByteBuf in) {
                        boolean isCompress = in.readBoolean();
                        CompositeByteBuf decompressed = in.alloc().compositeBuffer();
                        if (isCompress) {
                            EmbeddedChannel ch = decompressChannel.get();
                            ch.writeInbound(in.readRetainedSlice(in.readableBytes()));
                            ch.flushInbound();
                            ByteBuf buf;
                            while ((buf = ch.readInbound()) != null) {
                                decompressed.addComponent(true, buf);
                            }
                        } else {
                            decompressed.addComponent(true, in.readRetainedSlice(in.readableBytes()));
                        }
                        return decompressed;
                    }
                }
                """;

        RpcRequest rpcRequest = RpcRequest.withBody("tyuio");
        rpcRequest.setEnableCompress(true);
        client.sendRequest(rpcRequest);
        // 防止线程退出
        Thread.currentThread().join();
    }


}
