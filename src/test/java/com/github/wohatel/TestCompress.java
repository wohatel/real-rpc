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
                东方金信综合管理系统
                首页 客户管理 合同管理 项目管理 人事管理 培训管理 审批信息
                姚创
                修改密码个人信息 登出
                收起侧边栏
                项目管理/
                工时填报/
                工时审批
                待审批已审批
                """;


//        StringBuffer buffer = new StringBuffer();


        RpcRequest rpcRequest = RpcRequest.withBody(body);
        rpcRequest.setEnableCompress(true);
        client.sendRequest(rpcRequest);
        // 防止线程退出
        Thread.currentThread().join();
    }


}
