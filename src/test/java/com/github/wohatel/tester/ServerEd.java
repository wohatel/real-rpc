package com.github.wohatel.tester;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcMsgTransUtil;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.interaction.handler.RpcSessionRequestMsgHandler;
import com.github.wohatel.interaction.handler.RpcSimpleRequestMsgHandler;
import com.github.wohatel.tcp.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;

import java.io.File;

/**
 * description
 *
 * @author yaochuang 2025/09/15 14:26
 */
public class ServerEd {

    public static void main(String[] args) throws InterruptedException {
        MultiThreadIoEventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        RpcServer rpcServer = new RpcServer(8765, group, group);

        rpcServer.onFileReceive(new RpcFileReceiverHandler() {
            @Override
            public RpcFileLocal getTargetFile(RpcSession rpcSession, RpcSessionContext context, RpcFileInfo fileInfo) {
                return new RpcFileLocal(new File("/tmp/" + fileInfo.getFileName()));
            }
        });

        rpcServer.onMsgReceive(new RpcSimpleRequestMsgHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcRequest request) {
                if (request.isNeedResponse()) {
                    RpcResponse response = request.toResponse();
                    response.setBody("可");
                    RpcMsgTransUtil.write(ctx.channel(), response);
                }
            }
        });

        rpcServer.onSessionMsgReceive(new RpcSessionRequestMsgHandler() {


            /**
             * 注意,一旦处理消息较为耗时,会影响其它消息的消费,建议使用异步线程处理读取逻辑
             *
             * @param ctx
             * @param rpcSession
             */
            public boolean sessionStart(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context) {
                System.out.println("收到开启");
                return true;
            }


            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request, RpcSessionContext context) {
                System.out.println(request.getBody());
                RpcMsgTransUtil.sendSessionFinishRequest(ctx.channel(), rpcSession);
            }


            /**
             * 接收到对方发来的结束会话请求
             *
             * @param rpcSession 会话
             */
            public void sessionStop(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context) {
                System.out.println("对方销毁session");
            }
        });


        rpcServer.start().sync();
    }

}
