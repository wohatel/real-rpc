package com.github.wohatel;

import com.github.wohatel.udp.RpcUdpHeartSpider;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

/**
 * description
 *
 * @author yaochuang 2025/09/28 11:15
 */
public class TestRpcUdpHeartSpider {


    @Test
    void sendHeart() throws InterruptedException {
        RpcUdpHeartSpider spider1 = new RpcUdpHeartSpider(new NioEventLoopGroup(), null, 3000L, 10_000L);
        RpcUdpHeartSpider spider2 = new RpcUdpHeartSpider(new NioEventLoopGroup(), null, 3000L, 10_000L);


        spider1.bind(8765);
        spider2.bind(8766);


        spider1.addRemoteSocket(new InetSocketAddress("127.0.0.1", 8766));
//        spider2.addRemoteSocket(new InetSocketAddress("127.0.0.1", 8765));


        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                boolean alive = spider1.getRemoteSocket(new InetSocketAddress("127.0.0.1", 8766)).isAlive();
                System.out.println(alive);
            }
        });


        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                spider1.close();
            }
        });

        Thread.currentThread().join();
    }


}
