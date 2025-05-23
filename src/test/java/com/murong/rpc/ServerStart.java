package com.murong.rpc;


import com.murong.rpc.server.RpcServer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.PlatformDependent;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;
public class ServerStart {
    public static void start() throws Exception {
        RpcServer rpcServer = new RpcServer(8888, new NioEventLoopGroup(), new NioEventLoopGroup());
        rpcServer.start();
    }
    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            while (true) {
                Field field = null;
                try {
                    Thread.sleep(1000);
                    field = PlatformDependent.class.getDeclaredField("DIRECT_MEMORY_COUNTER");
                    field.setAccessible(true);
                    AtomicLong directMemory = ((AtomicLong) field.get(PlatformDependent.class));
                    System.out.println("文件大小:" + directMemory);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();
        start();
    }

}
