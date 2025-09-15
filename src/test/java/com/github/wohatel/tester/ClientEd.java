package com.github.wohatel.tester;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.tcp.RpcDefaultClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/09/15 14:26
 */
public class ClientEd {


    public static void main(String[] args) throws InterruptedException {
        MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

        RpcDefaultClient client = new RpcDefaultClient("127.0.0.1", 8765, group);

        client.onFileReceive(new RpcFileReceiverHandler() {
            @Override
            public RpcFileLocal getTargetFile(RpcSession rpcSession, RpcSessionContext context, RpcFileInfo fileInfo) {
                return new RpcFileLocal(new File("/tmp/" + fileInfo.getFileName()));
            }
        });

        client.onMsgReceive(new RpcSimpleRequestMsgHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                if (request.isNeedResponse()) {
                    RpcResponse response = request.toResponse();
                    response.setBody("可");
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }
            }
        });

        client.onSessionMsgReceive(new RpcSessionRequestMsgHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request, RpcSessionContext context) {
                System.out.println(request.getBody());
                RpcResponse response = request.toResponse();
                response.setBody("可");
                RpcMsgTransUtil.write(ctx.channel(), response);
            }

            public void sessionStop(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context) {
                System.out.println();
            }
        });


        client.connect().sync();
        RpcSession session = new RpcSession(10000);
        RpcSessionFuture rpcSessionFuture = client.startSession(session);

        while (true) {
            client.sendSessionMsg(new RpcSessionRequest(session));
            Thread.sleep(1000);

            boolean isRunning = client.inquiryServerSession(session);

            System.out.println("服务端的session是否开启?:" + isRunning);
        }


//        Thread.currentThread().join();
    }
}
