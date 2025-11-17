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

/**
 * A class representing a future for RPC (Remote Procedure Call) operations.
 * Uses Lombok annotations for getter/setter generation and method chaining.
 */
@Accessors(chain = true)
public class RpcFuture {

    /**
     * The underlying CompletableFuture that handles the asynchronous operation result
     */
    private final CompletableFuture<RpcReaction> completableFuture;
    /**
     * Maximum time allowed for the operation to complete
     */
    @Getter
    @Setter
    private long timeOut;
    /**
     * Timestamp when the request was initiated
     */
    @Getter
    @Setter
    private long requestTime;
    /**
     * Timestamp when the reaction was received
     */
    @Setter
    @Getter
    private long reactionTime;
    /**
     * Unique identifier for this future
     */
    @Getter
    @Setter
    private String futureId;

    /**
     * List of listeners that will be notified when the reaction is received
     */
    @Getter
    private List<RpcReactionMsgListener> listeners;

    /**
     * Constructor for RpcFuture
     *
     * @param timeOut the maximum time allowed for the operation to complete
     */
    public RpcFuture(long timeOut) {
        this.timeOut = timeOut;
        this.requestTime = System.currentTimeMillis();
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

    /**
     * Retrieves the RPC reaction with a default timeout.
     * This method is annotated with @SneakyThrows to automatically throw checked exceptions.
     *
     * @return The RPC reaction after the specified timeout period
     */
    @SneakyThrows
    public RpcReaction get() {
        // Delegate to the overloaded get method with default timeout in milliseconds
        return this.get(timeOut, TimeUnit.MILLISECONDS);
    }

    /**
     * Retrieves the result of the RPC reaction with a specified timeout.
     * This method uses the @SneakyThrows annotation to automatically throw checked exceptions.
     *
     * @param timeMills the maximum time to wait
     * @param timeUnit  the time unit of the timeMills parameter
     * @return the Rpc reaction result
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws ExecutionException   if the computation threw an exception
     * @throws TimeoutException     if the timeout expires before the computation completes
     */
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

    /**
     * This method checks if the CompletableFuture has been completed.
     *
     * @return true if the CompletableFuture has completed, either normally or exceptionally; false otherwise.
     */
    public boolean isDone() {
        return completableFuture.isDone(); // Return the completion status of the CompletableFuture
    }

    /**
     * Releases resources and cleans up the RPC future by removing it from the manager and canceling the associated CompletableFuture.
     * This method is typically called when the RPC call is completed or needs to be terminated.
     */
    public void release() {
        // Remove the future from the RpcFutureTransManager using its future ID
        RpcFutureTransManager.remove(this.getFutureId());
        // Cancel the CompletableFuture, potentially interrupting the ongoing operation if it's running
        completableFuture.cancel(true);
    }

}