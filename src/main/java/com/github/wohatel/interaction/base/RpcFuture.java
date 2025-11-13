package com.github.wohatel.interaction.base;

import com.github.wohatel.interaction.common.RpcFutureTransManager;
import com.github.wohatel.interaction.common.RpcReactionMsgListener;
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

    private final CompletableFuture<RpcReaction> completableFuture;
    @Getter
    @Setter
    private long timeOut;
    @Getter
    @Setter
    private long requestTime = System.currentTimeMillis();
    @Getter
    private long reactionTime;
    @Getter
    @Setter
    private String futureId;

    @Getter
    private List<RpcReactionMsgListener> listeners;

    public RpcFuture(long timeOut) {
        this.timeOut = timeOut;
        this.completableFuture = new CompletableFuture<>();
    }

    /**
     * listen reaction to this future
     * if this future instanceOf RpcSessionFuture: then when RpcSessionProcess.RUNNING
     */
    public synchronized RpcFuture addListener(RpcReactionMsgListener rpcReactionMsgListener) {
        if (rpcReactionMsgListener != null) {
            if (listeners == null) {
                listeners = new ArrayList<>();
            }
            listeners.add(rpcReactionMsgListener);
        }
        return this;
    }

    public void flushRequestTime() {
        RpcFutureTransManager.flushTime(futureId, timeOut);
    }

    public void setReactionTime(long reactionTime) {
        this.reactionTime = reactionTime;
        RpcFutureTransManager.flushTime(futureId, timeOut);
    }

    @SneakyThrows
    public RpcReaction get() {
        return this.get(timeOut, TimeUnit.MILLISECONDS);
    }

    @SneakyThrows
    public RpcReaction get(long timeMills, TimeUnit timeUnit) {
        return this.completableFuture.get(timeMills, timeUnit);
    }

    /**     
     * Tell CompletableFuture that the reaction is over,
     * and process it when RpcFuture.get is done
     */
    public void complete(RpcReaction reaction) {
        completableFuture.complete(reaction);
    }

    public boolean isDone() {
        return completableFuture.isDone();
    }

    public void release() {
        RpcFutureTransManager.remove(this.getFutureId());
        completableFuture.cancel(true);
    }

}