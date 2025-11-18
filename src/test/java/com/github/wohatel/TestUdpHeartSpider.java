package com.github.wohatel;

import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.udp.RpcUdpHeartSpider;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

/**

 *
 * @author yaochuang 2025/09/28 11:15
 */
public class TestUdpHeartSpider {


    @Test
    void sendHeart() throws InterruptedException {
        RpcUdpHeartSpider spider1 = new RpcUdpHeartSpider();
        RpcUdpHeartSpider spider2 = new RpcUdpHeartSpider();

        spider1.bind(8765);
        spider2.bind(8766);


        // spider 添加一个远程的主机,并测试联通性
        spider1.addRemoteSocket(new InetSocketAddress("127.0.0.1", 8766));

        // 同时可以发送其它消息
        spider1.sendMsg(RpcRequest.withBody("hello"), new InetSocketAddress("127.0.0.1", 8766));

        // 开启一个线程监控 spider1 对于容器内的主机的连通性
        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                boolean alive = spider1.getRemoteSocket(new InetSocketAddress("127.0.0.1", 8766)).isAlive();
                System.out.println(alive);

                if (!alive) {
                    System.out.println("无法发出的消息");
                    spider1.sendMsg(RpcRequest.withBody("hello"), new InetSocketAddress("127.0.0.1", 8766));
                }
            }
        });


        // 15s后关闭 spider2 主机,关闭后,上个线程在又等了10s后,改变状态,认为其已经超时或连接中断
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            spider2.close();
        });

        Thread.currentThread().join();
    }


}
