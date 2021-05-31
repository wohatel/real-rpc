package com.murong.rpc.test;


import com.murong.rpc.client.RpcAutoReconnectClient;
import com.murong.rpc.interaction.RpcFileRequest;
import com.murong.rpc.interaction.RpcFuture;
import com.murong.rpc.interaction.RpcMsgTransUtil;
import com.murong.rpc.interaction.ThreadUtil;
import com.murong.rpc.util.ArrayUtil;
import org.bouncycastle.util.Arrays;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ReconnectClientSendFile {

    public static void main(String[] args) throws Exception {
        RpcAutoReconnectClient client = new RpcAutoReconnectClient("127.0.0.1", 8888);
        client.reConnect();

        Thread.sleep(2000);

        client.sendFile("/Users/yaochuang/test/abc.tar.gz");
    }
}
