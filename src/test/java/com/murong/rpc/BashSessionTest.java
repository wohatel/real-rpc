package com.murong.rpc;

import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.common.BashSession;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.common.SessionManager;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * description
 *
 * @author yaochuang 2025/04/27 11:19
 */
public class BashSessionTest {


    public static void main(String[] args) throws Exception {

        /**
         * 测试流程
         * 1: 开启服务端
         * 2: 开启客户端,扫描控制台输入:
         * 3: 流程逻辑:
         *      client发送一个sessionStart请求-->服务端收到sessionStart请求就开启一个BashSession,同时响应告知client开始;同时开启线程端不断的监听标准输出和错误输出,异步的给发送给client
         *      client设置的监听器就是收到相应就打印body
         *      client 扫描控制台输入,的向server发送session内请求-->server接到session内请求shell命令,就找到对应的BashSession
         *      client 会不断的接受到server端的响应结果,然后打印出来
         */

        start();
        Thread.sleep(3000);
        clientConnect();
    }

    public static void clientConnect() throws IOException {

        RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
        defaultClient.connect();

        RpcSession session = new RpcSession(10_000);
        RpcSessionFuture objectRpcSessionFuture = defaultClient.startSession(session);
        RpcResponse rpcResponse = objectRpcSessionFuture.get();
        System.out.println(rpcResponse + ":建立session");
        // 同时设置监听
        objectRpcSessionFuture.addListener(res -> {
            System.out.println(res.getBody());
        });

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            // 输入 exit 则退出
            if ("exit".equalsIgnoreCase(line.trim())) {
                System.out.println("再见 👋");
                RpcSessionRequest request = new RpcSessionRequest(session);
                request.setCommand(line);
                defaultClient.sendSessionMsg(request);
            } else if ("close".equalsIgnoreCase(line.trim())) {
                System.out.println("关闭服务端");
                RpcSessionRequest request = new RpcSessionRequest(session);
                request.setCommand(line);
                defaultClient.sendSessionMsg(request);
            } else if (line.equals("new session")) {
                session = new RpcSession(300_000);
                defaultClient.startSession(session).addListener(res -> {
                    System.out.println(res.getBody());
                });
            } else if (!StringUtil.isNullOrEmpty(line)) {
                RpcSessionRequest request = new RpcSessionRequest(session);
                request.setCommand(line);
                defaultClient.sendSessionMsg(request);
            }
        }
    }


    public static void start() throws Exception {


        SessionManager<BashSession> sessionSessionManager = new SessionManager<>(10_000, BashSession::close);
        RpcServer rpcServer = new RpcServer(8765, new NioEventLoopGroup(), new NioEventLoopGroup());
        RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler = new RpcSessionRequestMsgHandler() {
            @Override
            public void sessionStart(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {
                BashSession session = sessionSessionManager.getSession(rpcSession.getSessionId());
                if (session == null) {
                    BashSession bashSession = new BashSession(rs -> {
                        RpcResponse innser = rpcSession.toResponse();
                        innser.setBody(rs);
                        RpcMsgTransUtil.write(ctx.channel(), innser);
                    });
                    sessionSessionManager.initSession(rpcSession.getSessionId(), bashSession);
                }
                RpcResponse response = rpcSession.toResponse();
                // 默认就是成功
                RpcMsgTransUtil.write(ctx.channel(), response);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionRequest request, RpcSessionContext context) {
                String command = request.getCommand();
                BashSession session = sessionSessionManager.getSession(rpcSession.getSessionId());
                sessionSessionManager.flushTime(rpcSession.getSessionId());
                if (request.getCommand().equals("exit")) {
                    System.out.println("退出session");
                    sessionSessionManager.release(rpcSession.getSessionId());
                    BashSession session1 = sessionSessionManager.getSession(rpcSession.getSessionId());
                    System.out.println(session1);
                } else if (request.getCommand().equals("close")) {
                    System.out.println("关闭server");
                    rpcServer.close();
                } else if (request.getCommand().equals("history")) {
                    String history = session.history();
                    RpcResponse response = request.toResponse();
                    response.setBody(history);
                    RpcMsgTransUtil.write(ctx.channel(), response);
                } else if (request.getCommand().equals("skip")) {
                    Long foregroundProcess = session.findForegroundProcess();
                    if (foregroundProcess != null) {
                        session.kill9Pid(foregroundProcess);
                    }
                    RpcResponse response = request.toResponse();
                    response.setBody("ok");
                    RpcMsgTransUtil.write(ctx.channel(), response);
                } else {
                    session.sendCommand(command);
                }
            }

            @Override
            public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession, RpcSessionContext context) {

            }

        };
        rpcServer.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
        rpcServer.start();
    }


}
