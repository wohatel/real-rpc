package com.murong.rpc.interaction.file;

import com.murong.rpc.interaction.base.RpcSession;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 文件发送方处理文件传输事件
 *
 * @author yaochuang 2025/03/27 13:29
 */
public class RpcFileSenderWrapper {

    @Getter
    private final RpcFileSendEvents rpcFileSendEvents;

    @Getter
    private final RpcSession rpcSession;

    @Getter
    private final RpcFileSendDesc rpcFileSendDesc;

    public RpcFileSenderWrapper(RpcSession rpcSession, RpcFileSendDesc rpcFileSendDesc) {
        this.rpcSession = rpcSession;
        this.rpcFileSendDesc = rpcFileSendDesc;
        this.rpcFileSendEvents = new RpcFileSendEvents();
    }

    @RequiredArgsConstructor
    public static class RpcFileSendDesc {
        @Getter
        private final File localFile;
        @Getter
        private final RpcFileTransModel transModel;
    }

    public static class RpcFileSendEvents {
        @Getter
        private final List<BiConsumer<RpcFileSendDesc, String>> failureEvents = new ArrayList<>();

        @Getter
        private final List<BiConsumer<RpcFileSendDesc, RpcFileTransProcess>> processEvents = new ArrayList<>();

        @Getter
        private final List<Consumer<RpcFileSendDesc>> successEvents = new ArrayList<>();
    }

    /**
     * 传输失败后触发
     */
    public RpcFileSenderWrapper onFailure(final BiConsumer<RpcFileSendDesc, String> consumer) {
        this.rpcFileSendEvents.getFailureEvents().add(consumer);
        return this;
    }

    /**
     * 监控传输速度
     */
    public RpcFileSenderWrapper onProcess(final BiConsumer<RpcFileSendDesc, RpcFileTransProcess> consumer) {
        this.rpcFileSendEvents.getProcessEvents().add(consumer);
        return this;
    }

    /**
     * 传输成功触发
     */
    public RpcFileSenderWrapper onSuccess(final Consumer<RpcFileSendDesc> consumer) {
        this.rpcFileSendEvents.getSuccessEvents().add(consumer);
        return this;
    }
}
