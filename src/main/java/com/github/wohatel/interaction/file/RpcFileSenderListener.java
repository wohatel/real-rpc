package com.github.wohatel.interaction.file;


public interface RpcFileSenderListener {

    default void onSuccess(RpcFileSenderWrapper rpcFileSenderWrapper) {

    }

    default void onFailure(RpcFileSenderWrapper rpcFileSenderWrapper, String errorMsg) {

    }

    default void onProcess(RpcFileSenderWrapper rpcFileSenderWrapper, RpcFileTransProcess process) {
    }

}
