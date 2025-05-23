package com.murong.rpc;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.client.RpcDefaultClient;
import com.murong.rpc.client.RpcHeartClient;
import com.murong.rpc.interaction.common.BashSession;
import com.murong.rpc.interaction.common.SessionManager;
import com.murong.rpc.interaction.common.RpcMsgTransUtil;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.handler.RpcHeartTimeOutHandler;
import com.murong.rpc.interaction.handler.RpcSessionRequestMsgHandler;
import com.murong.rpc.server.RpcServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * description
 *
 * @author yaochuang 2025/04/27 11:19
 */
public class BashSessionTest {

    public static void start() throws Exception {


        SessionManager<BashSession> sessionSessionManager = new SessionManager<>(3000_000, BashSession::close);
        RpcServer rpcServer = new RpcServer(8765, new NioEventLoopGroup(), new NioEventLoopGroup());
        RpcSessionRequestMsgHandler rpcSessionRequestMsgHandler = new RpcSessionRequestMsgHandler() {
            @Override
            public void sessionStart(ChannelHandlerContext ctx, RpcSession rpcSession, JSONObject context) {
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
                response.setBody("开始");
                RpcMsgTransUtil.write(ctx.channel(), response);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, RpcSession rpcSession, JSONObject context, RpcSessionRequest request) {
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
            public void sessionStop(ChannelHandlerContext ctx, RpcSession rpcSession, JSONObject context) {

            }

        };
        rpcServer.setRpcSessionRequestMsgHandler(rpcSessionRequestMsgHandler);
        rpcServer.start();
    }

    public static void main(String[] args) throws Exception {
        // 构建一个30s的session,如果30s内没有客户端操作,则给与停止
        start();

        Thread.sleep(3000);
        RpcHeartClient heartclient = new RpcHeartClient("127.0.0.1", 8765, new RpcHeartTimeOutHandler() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) {
                System.out.println("链接异常");
            }
        });
        heartclient.connect();


        RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765);
        defaultClient.connect();

        RpcSession session = new RpcSession(3000_000);
        RpcSessionFuture objectRpcSessionFuture = defaultClient.startSession(session, null);
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
                defaultClient.startSession(session, null).addListener(res -> {
                    System.out.println(res.getBody());
                });
            } else if (!StringUtil.isNullOrEmpty(line)) {
                RpcSessionRequest request = new RpcSessionRequest(session);
                request.setCommand(line);
                defaultClient.sendSessionMsg(request);
            }
        }
    }
}
