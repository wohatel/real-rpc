package com.github.wohatel;

import com.github.wohatel.tcp.RpcDefaultClient;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionRequest;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.handler.RpcResponseMsgListener;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * description
 *
 * @author yaochuang 2025/03/25 14:09
 */
public class SendSessionMsgClient {


    public static void main(String[] args) throws InterruptedException {
        clientConnect();
    }

    public static void clientConnect() throws InterruptedException {

        RpcDefaultClient defaultClient = new RpcDefaultClient("127.0.0.1", 8765, new NioEventLoopGroup());
        defaultClient.connect();

        Thread.sleep(100);

        RpcSession oo = new RpcSession(10000);
        System.out.println(oo.getSessionId());
        RpcSessionFuture rpcFuture = defaultClient.startSession(oo, new RpcSessionContext());
        RpcResponse response = rpcFuture.get();
        System.out.println(response.getBody());
        System.out.println(Thread.currentThread());
        System.out.println(response);

        rpcFuture.addListener(new RpcResponseMsgListener() {
            @Override
            public void onResponse(RpcResponse response) {
                System.out.println(response);
            }

            /**
             * 超时
             */
            @Override
            public void onTimeout() {
                System.out.println("超时");
            }
        });
        RpcSessionRequest rpcSessionRequest = oo.toRpcSessionRequest();

        rpcSessionRequest.setBody("你好");
        defaultClient.sendSessionMsg(rpcSessionRequest);
        Thread.sleep(1000);
        defaultClient.finishSession(oo);

        defaultClient.close();

    }

}
