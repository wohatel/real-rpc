package com.murong.rpc;

import com.murong.rpc.interaction.RpcFuture;
import com.murong.rpc.interaction.ThreadUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;


public class RealRpcApplicationTests {


    public static void main(String[] args) throws InterruptedException {


        ConcurrentHashMap<String, RpcFuture> map = new ConcurrentHashMap<>();

        ThreadUtil.run(30,()->{
            for (int i = 0; i < 1000000; i++) {

                map.put("" + i, new RpcFuture());
            }
        });




        Thread.sleep(1000);
        ThreadUtil.run(1,()->{
            while (true){

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Set<Map.Entry<String, RpcFuture>> entries = map.entrySet();
                Iterator<Map.Entry<String, RpcFuture>> iterator = entries.iterator();

                long l1 = System.currentTimeMillis();
                while (iterator.hasNext()) {
                    Map.Entry<String, RpcFuture> next = iterator.next();
                    String key = next.getKey();
                    if (Integer.parseInt(key) % 10 == 0) {
                        iterator.remove();
                    }
                }
                long l2 = System.currentTimeMillis();

                System.out.println(l2 - l1);
            }
        });

    }


}
