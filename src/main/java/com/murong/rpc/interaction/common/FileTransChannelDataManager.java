package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONArray;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.file.RpcFileLocal;
import com.murong.rpc.interaction.file.RpcFileTransWrapper;
import com.murong.rpc.interaction.file.RpcFileRequest;
import com.murong.rpc.interaction.file.RpcFileWrapperUtil;
import com.murong.rpc.interaction.handler.RpcFileReceiverHandler;
import com.murong.rpc.util.JsonUtil;
import com.murong.rpc.util.ReflectUtil;
import com.murong.rpc.util.RunnerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author yaochuang
 */
@Log
public class FileTransChannelDataManager {

    @SneakyThrows
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcFileReceiverHandler rpcFileReceiverHandler) {
        RpcFileRequest rpcFileRequest = rpcMsg.getPayload(RpcFileRequest.class);
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        if (rpcFileReceiverHandler == null) {
            sendStartError(rpcResponse, ctx.channel(), "远端接收文件事件暂无接收文件配置:发送终止");
            return;
        }
        if (rpcFileRequest.isSessionStart()) {
            boolean running = TransSessionManger.isRunning(rpcFileRequest.getRpcSession().getSessionId());
            if (running) {
                sendStartError(rpcResponse, ctx.channel(), "请勿开启重复session,请检查");
                return;
            }
            String body = rpcFileRequest.getBody();
            RpcSessionContext sessionContext = JsonUtil.fromJson(body, RpcSessionContext.class);
            RpcFileLocal rpcFileWrapper = rpcFileReceiverHandler.getTargetFile(ctx, rpcFileRequest.getRpcSession(), sessionContext, rpcFileRequest.getFileInfo());
            if (rpcFileWrapper == null) {
                sendStartError(rpcResponse, ctx.channel(), "远端接受文件路径错误:发送终止");
                return;
            }
            // 继续处理逻辑
            readInitFile(ctx, rpcFileRequest, sessionContext, rpcFileWrapper, rpcFileReceiverHandler);
        } else if (rpcFileRequest.isSessionFinish()) {
            RpcSession rpcSession = rpcFileRequest.getRpcSession();
            boolean running = TransSessionManger.isRunning(rpcSession.getSessionId());
            if (!running) {// 如果已经不再运行,则无需执行
                return;
            }
            RpcFileTransWrapper data = TransSessionManger.getFileData(rpcSession.getSessionId());
            TransSessionManger.release(rpcSession.getSessionId());
            VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onStop(ctx, rpcSession, data));
        } else {
            readBodyFile(ctx, rpcFileRequest, rpcMsg.getByteBuffer());
        }
    }

    @SneakyThrows
    private static void readBodyFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, ByteBuf byteBuf) {
        TransSessionManger.FileChunkItem item = new TransSessionManger.FileChunkItem();
        item.setByteBuf(byteBuf);
        item.setBuffer(rpcFileRequest.getBuffer());
        item.setSerial(rpcFileRequest.getSerial());
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        boolean addStatus = TransSessionManger.addOrReleaseFile(rpcSession.getSessionId(), item);
        if (!addStatus) {
            RpcResponse response = rpcFileRequest.toResponse();
            response.setSuccess(false);
            response.setMsg("停止接收文件块");
            RpcMsgTransUtil.write(ctx.channel(), response);
        }
    }

    private static void readInitFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcSessionContext context, RpcFileLocal fileLocalWrapper, RpcFileReceiverHandler rpcFileReceiverHandler) {
        RpcFileWrapperUtil fileWrapper = RpcFileWrapperUtil.fromLocalWrapper(fileLocalWrapper);
        fileWrapper.init(rpcFileRequest.getFileInfo().getLength());
        List<String> body = new ArrayList<>();
        body.add(String.valueOf(fileWrapper.isNeedTrans()));
        body.add(fileWrapper.getTransModel().name());
        body.add(String.valueOf(fileWrapper.getWriteIndex()));
        body.add(fileWrapper.getFile().getAbsolutePath());
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        rpcResponse.setBody(JSONArray.toJSONString(body));
        rpcResponse.setMsg(fileWrapper.getMsg());
        RpcMsgTransUtil.write(ctx.channel(), rpcResponse);
        if (fileWrapper.isInterruptByInit()) {
            RpcFileTransWrapper impl = new RpcFileTransWrapper(fileWrapper.getFile(), fileWrapper.getTransModel(), rpcFileRequest.getFileInfo(), context, 0L);
            VirtualThreadPool.execute(() -> rpcFileReceiverHandler.onSuccess(ctx, rpcFileRequest.getRpcSession(), impl));
            log.info("接收方文件接收结束: 无需传输");
        } else {
            VirtualThreadPool.execute(fileWrapper.isNeedTrans(), () -> handleAsynRecieveFile(ctx, rpcFileRequest, context, fileLocalWrapper, fileWrapper.getWriteIndex(), rpcFileReceiverHandler));
        }
    }

    @SneakyThrows
    private static void handleAsynRecieveFile(ChannelHandlerContext ctx, final RpcFileRequest rpcFileRequest, final RpcSessionContext context, final RpcFileLocal fileWrapper, long index, final RpcFileReceiverHandler rpcFileReceiverHandler) {
        File targetFile = fileWrapper.getFile();
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        long length = rpcFileRequest.getFileInfo().getLength() - index;
        long chunkSize = rpcFileRequest.getBuffer();
        long chunks = (length + chunkSize - 1) / chunkSize;
        RpcResponse response = rpcFileRequest.toResponse();
        RpcFileTransWrapper impl = new RpcFileTransWrapper(fileWrapper.getFile(), fileWrapper.getTransModel(), rpcFileRequest.getFileInfo(), context, length);
        TransSessionManger.initFile(rpcSession.getSessionId(), NumberConstant.SEVENTY_FIVE, impl, rpcFileRequest.getRpcSession());
        boolean isProcessOverride = ReflectUtil.isOverridingInterfaceDefaultMethod(rpcFileReceiverHandler.getClass(), "onProcess");
        try {
            AtomicInteger handleChunks = new AtomicInteger();
            // 以追加模式打开目标文件
            try (FileOutputStream fos = new FileOutputStream(targetFile, true); FileChannel fileChannel = fos.getChannel()) {
                for (int i = 0; i < chunks; i++) {
                    TransSessionManger.FileChunkItem poll = RunnerUtil.tryTimesUntilNotNull(() -> TransSessionManger.isRunning(rpcSession.getSessionId()), 3, () -> TransSessionManger.poll(rpcSession.getSessionId(), rpcSession.getTimeOutMillis() / 3));
                    // 拉取之后也要判断是否正常
                    if (poll == null) {
                        if (!TransSessionManger.isRunning(rpcSession.getSessionId())) {
                            break;
                        }
                        throw new RuntimeException("文件块接收超时");
                    }
                    if (i != poll.getSerial()) {
                        throw new RuntimeException("文件块丢失:" + i);
                    }
                    ByteBuf byteBuf = poll.getByteBuf();
                    try {
                        int readableBytes = byteBuf.readableBytes();
                        byteBuf.getBytes(byteBuf.readerIndex(), fileChannel, readableBytes);
                    } finally {
                        if (byteBuf.refCnt() > 0) {
                            ReferenceCountUtil.safeRelease(byteBuf);
                        }
                    }
                    long recieveSize = i != chunks - 1 ? (i + 1) * chunkSize : length;
                    response.setBody(String.valueOf(recieveSize));
                    RpcMsgTransUtil.write(ctx.channel(), response);
                    if (isProcessOverride) {
                        // 同步执行
                        RunnerUtil.execSilent(() -> rpcFileReceiverHandler.onProcess(ctx, rpcSession, impl, recieveSize));
                    }
                    handleChunks.incrementAndGet();
                }
                if (handleChunks.get() == chunks) {
                    RunnerUtil.execSilent(() -> rpcFileReceiverHandler.onSuccess(ctx, rpcSession, impl));
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "异常", e);
            response.setMsg(e.getMessage());
            response.setSuccess(false);
            RpcMsgTransUtil.write(ctx.channel(), response);
            RunnerUtil.execSilent(() -> rpcFileReceiverHandler.onFailure(ctx, rpcSession, impl, e));
        } finally {
            TransSessionManger.release(rpcSession.getSessionId());
        }
    }

    private static void sendStartError(RpcResponse rpcResponse, Channel channel, String message) {
        List<String> rbody = new ArrayList<>();
        rbody.add(String.valueOf(false));
        rbody.add(null);
        rbody.add(String.valueOf(0));
        rbody.add(null);
        rpcResponse.setBody(JSONArray.toJSONString(rbody));
        rpcResponse.setMsg(message);
        RpcMsgTransUtil.write(channel, rpcResponse);
    }
}