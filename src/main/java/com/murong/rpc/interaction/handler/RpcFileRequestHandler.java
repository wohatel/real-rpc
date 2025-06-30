package com.murong.rpc.interaction.handler;

import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileWrapper;

/**
 * 文件接收端处理事件接口
 *
 * @author yaochuang 2025/03/28 09:44
 */
public interface RpcFileRequestHandler {

    /**
     * 确定文件存储位置（在文件传输开始前调用）
     *
     * @param context 文件上下文
     * @return 存储文件对象（如果返回 null，表示拒绝存储）
     */
    RpcFileWrapper getTargetFile(final RpcFileContext context);

    /**
     * 远程接收文件的进度,每次有变化就会触发
     * <p>
     * 注意: 如果处理逻辑比较久,建议异步操作
     *
     * @param context      文件上下文
     */
    default void onProcess(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper, long recieveSize) {

    }

    /**
     * 文件接收异常执行
     *
     * @param context 文件上下文
     * @param e       发生的异常
     */
    default void onFailure(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper, final Exception e) {

    }

    /**
     * 文件整体传输完毕
     *
     * @param context 文件上下文
     */
    default void onSuccess(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper) {

    }

    /**
     * 远端发出终止传输信号
     *
     * @param context 文件上下文
     */
    default void onStop(final RpcFileContext context, final RpcFileWrapper rpcFileWrapper) {

    }
}