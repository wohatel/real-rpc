package com.github.wohatel.interaction.base;


import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.common.RpcInteractionContainer;
import com.github.wohatel.interaction.handler.RpcResponseMsgListener;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Accessors(chain = true)
public class RpcFuture {

    private final CompletableFuture<RpcResponse> completableFuture = new CompletableFuture<>();
    @Getter
    @Setter
    private long timeOut = NumberConstant.OVER_TIME;
    @Getter
    @Setter
    private long requestTime = System.currentTimeMillis();
    @Getter
    private long responseTime;
    @Getter
    @Setter
    private String requestId;
    @Getter
    @Setter
    private RpcResponse response;

    @Getter
    private List<RpcResponseMsgListener> listeners;

    public RpcFuture(long timeOut) {
        this.timeOut = timeOut;
    }

    public RpcFuture() {
    }

    public synchronized RpcFuture addListener(RpcResponseMsgListener rpcResponseMsgListener) {
        if (rpcResponseMsgListener != null) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(rpcResponseMsgListener);
        }
        return this;
    }

    public void flushRequestTime() {
        RpcInteractionContainer.flushTime(requestId, timeOut);
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
        RpcInteractionContainer.flushTime(requestId, timeOut);
    }

    @SneakyThrows
    public RpcResponse get() {
        return this.get(timeOut, TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    public RpcResponse get(long timeMills, TimeUnit timeUnit) {
        return this.completableFuture.get(timeMills, timeUnit);
    }

    /**
     * 告知CompletableFuture响应结束,在RpcFuture.get的时候处理
     *
     * @param response
     */
    public void complete(RpcResponse response) {
        this.response = response;
        completableFuture.complete(response);
    }

    /**
     * 是否完成
     */
    public boolean isDone() {
        return completableFuture.isDone();
    }

    public void release() {
        RpcInteractionContainer.remove(this.getRequestId());
        completableFuture.cancel(true);
    }
}