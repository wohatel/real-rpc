package com.murong.rpc;


import com.murong.rpc.client.RpcAutoReconnectClient;

import java.io.IOException;

public class ReconnectClientSendFile {

    public void test() throws InterruptedException, IOException {
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8888);
        client.reConnect();

        Thread.sleep(2000);

        client.sendFile("/Users/yaochuang/test/abc.tar.gz");
    }
}
