package com.murong.rpc.interaction.handler;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.common.RpcSessionContext;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileLocal;
import com.murong.rpc.interaction.file.RpcFileTransWrapper;
import io.netty.channel.ChannelHandlerContext;

/**
 * 文件接收端处理事件接口
 *
 * @author yaochuang 2025/03/28 09:44
 */
public interface RpcFileReceiverHandler {

    /**
     * 确定文件存储位置（在文件传输开始前调用）
     *
     * @param context 文件上下文
     * @return 存储文件对象（如果返回 null，表示拒绝存储）
     */
    RpcFileLocal getTargetFile(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcSessionContext context, final RpcFileInfo fileInfo);

    /**
     * 远程接收文件的进度,每次有变化就会触发
     * <p>
     * 注意: 如果处理逻辑比较久,建议异步操作
     *
     */
    default void onProcess(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcFileTransWrapper rpcFileWrapper, long recieveSize) {

    }

    /**
     * 文件接收异常执行
     *
     * @param e       发生的异常
     */
    default void onFailure(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcFileTransWrapper rpcFileWrapper, final Exception e) {

    }

    /**
     * 文件整体传输完毕
     *
     */
    default void onSuccess(ChannelHandlerContext ctx, final RpcSession rpcSession,  final RpcFileTransWrapper rpcFileWrapper) {

    }

    /**
     * 远端发出终止传输信号
     *
     */
    default void onStop(ChannelHandlerContext ctx, final RpcSession rpcSession, final RpcFileTransWrapper rpcFileWrapper) {

    }
}