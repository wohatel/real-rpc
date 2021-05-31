package com.murong.rpc.interaction;

import com.alibaba.fastjson.JSON;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RpcInteractionContainer {
    private static final ConcurrentHashMap<String, RpcFuture> futureMap = new ConcurrentHashMap<>();

    public static RpcFuture addRequest(RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            return null;
        }
        if (rpcRequest.getRequestId() == null) {
            return null;
        }
        RpcFuture rpcFuture = new RpcFuture();
        rpcFuture.setRequestId(rpcRequest.getRequestId());
        futureMap.put(rpcRequest.getRequestId(), rpcFuture);
        return rpcFuture;
    }

    public static void addResponse(RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            return;
        }
        if (rpcResponse.getRequestId() == null) {
            return;
        }
        RpcFuture rpcFuture = futureMap.remove(rpcResponse.getRequestId());
        if (rpcFuture == null) { // 可能超时已被移除
            return;
        }
        rpcFuture.setReponseTime(System.currentTimeMillis());
        rpcFuture.setResponse(rpcResponse);
        rpcFuture.setDone(true);
    }

    public static void addResponse(String rpcResponseString) {
        if (rpcResponseString == null) {
            return;
        }
        addResponse(JSON.parseObject(rpcResponseString, RpcResponse.class));
    }

    public static RpcFuture addRequest(RpcRequest rpcRequest, long timeOut) {
        if (rpcRequest == null) {
            return null;
        }
        if (rpcRequest.getRequestId() == null) {
            return null;
        }
        RpcFuture rpcFuture = new RpcFuture();
        rpcFuture.setRequestId(rpcRequest.getRequestId());
        if (timeOut > 0) {
            rpcFuture.setTimeOut(timeOut);
        }
        futureMap.put(rpcRequest.getRequestId(), rpcFuture);
        return rpcFuture;
    }

    public static RpcFuture remove(String requestId) {
        return futureMap.remove(requestId);
    }


    public static ConcurrentHashMap<String, RpcFuture> getFutureMap() {
        return futureMap;
    }

    public static int concurrentSize() {
        if (futureMap == null) {
            return 0;
        }
        return futureMap.size();
    }

}
