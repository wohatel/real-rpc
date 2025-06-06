package com.murong.rpc.interaction.base;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcFuture;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yaochuang
 */
@Accessors(chain = true)
@Data
public class RpcSessionFuture extends RpcFuture {
    private final JSONArray context = new JSONArray();

    private volatile boolean isSessionFinish;

    public RpcSessionFuture() {
    }

    public RpcSessionFuture(long timeOut) {
        this.setTimeOut(timeOut);
    }

}
