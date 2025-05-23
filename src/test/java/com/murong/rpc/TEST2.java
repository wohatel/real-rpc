package com.murong.rpc;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.file.RpcFileRequest;

/**
 * description
 *
 * @author yaochuang 2025/05/20 15:28
 */
public class TEST2 {

    public static void main(String[] args) {

        RpcSession rpcSession = new RpcSession(System.currentTimeMillis());
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        rpcFileRequest.setFinished(true);
        rpcFileRequest.setLength(10);
        rpcFileRequest.setBody("ok");

        String jsonString = JSONObject.toJSONString(rpcFileRequest);

        System.out.println(jsonString);
        RpcFileRequest rpcFileRequest1 = JSONObject.parseObject(jsonString, RpcFileRequest.class);


        System.out.println(rpcFileRequest1);


    }

}
