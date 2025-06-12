package com.murong.rpc;

import com.murong.rpc.interaction.common.SessionManager;
import io.netty.channel.Channel;

import java.util.Random;
import java.util.UUID;


/**
 * description
 *
 * @author yaochuang 2025/06/12 17:17
 */
public class ChannelSessionTest {

    public static void main(String[] args) throws InterruptedException {
        SessionManager<String> channelSessionManager = new SessionManager<>(1_000, t -> System.out.println(t + "退出"));

        channelSessionManager.setAutoFlushPredicate((id, r) -> {
            long l = System.currentTimeMillis() % 3;

            System.out.println(id + "此时为继续:" + l);
            return l != 0;
        });


        for (int i = 0; i < 100; i++) {
            Thread.sleep(new Random().nextInt(100));
            channelSessionManager.initSession(UUID.randomUUID().toString(), i + "a");
        }

        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("剩余数量" + channelSessionManager.getContainer().size());
            }
        });

        Thread.sleep(10000000);

    }

}
