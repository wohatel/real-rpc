package com.github.wohatel.interaction.common;

import com.alibaba.fastjson2.JSONArray;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.interaction.file.RpcFileWrapperUtil;
import com.github.wohatel.interaction.handler.RpcFileReceiverHandler;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.ReflectUtil;
import com.github.wohatel.util.RunnerUtil;
import com.github.wohatel.util.VirtualThreadPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author yaochuang
 */
@Slf4j
public class RpcFileChannelDataTransManager {

    @SneakyThrows
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcFileReceiverHandler rpcFileReceiverHandler) {
        RpcFileRequest rpcFileRequest = rpcMsg.getPayload(RpcFileRequest.class);
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        if (rpcFileReceiverHandler == null) {
            sendStartError(rpcResponse, ctx.channel(), "There is no receive file configuration for remote receive file events: Transmission terminated");
            return;
        }
        if (rpcFileRequest.isSessionStart()) {
            boolean running = RpcSessionTransManger.isRunning(rpcFileRequest.getRpcSession().getSessionId());
            if (running) {
                sendStartError(rpcResponse, ctx.channel(), "Do not enable repeat sessions, please check");
                return;
            }
            String body = rpcFileRequest.getBody();
            RpcSessionContext sessionContext = JsonUtil.fromJson(body, RpcSessionContext.class);
            try {
                RpcFileLocal rpcFileWrapper = rpcFileReceiverHandler.getTargetFile(rpcFileRequest.getRpcSession(), sessionContext, rpcFileRequest.getFileInfo());
                if (rpcFileWrapper == null) {
                    sendStartError(rpcResponse, ctx.channel(), "Remote accept file path error: send terminated");
                    return;
                }
                // 继续处理逻辑
                readInitFile(ctx, rpcFileRequest, sessionContext, rpcFileWrapper, rpcFileReceiverHandler);
            } catch (Exception e) {
                sendStartError(rpcResponse, ctx.channel(), e.getMessage());
            }
        } else if (rpcFileRequest.isSessionFinish()) {
            RpcSession rpcSession = rpcFileRequest.getRpcSession();
            boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
            if (!running) {// 如果已经不再运行,则无需执行
                return;
            }
            RpcSessionTransManger.release(rpcSession.getSessionId());
        } else {
            readBodyFile(ctx, rpcFileRequest, rpcMsg.getByteBuffer());
        }
    }

    @SneakyThrows
    private static void readBodyFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, ByteBuf byteBuf) {
        RpcSessionTransManger.FileChunkItem item = new RpcSessionTransManger.FileChunkItem();
        item.setByteBuf(byteBuf);
        item.setBuffer(rpcFileRequest.getBuffer());
        item.setSerial(rpcFileRequest.getSerial());
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        boolean addStatus = RpcSessionTransManger.addOrReleaseFile(rpcSession.getSessionId(), item);
        if (!addStatus) {
            RpcResponse response = rpcFileRequest.toResponse();
            response.setSuccess(false);
            response.setMsg("Stop receiving file blocks");
            RpcMsgTransUtil.write(ctx.channel(), response);
        }
    }

    private static void readInitFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcSessionContext context, RpcFileLocal fileLocalWrapper, RpcFileReceiverHandler rpcFileReceiverHandler) {
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
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
        rpcResponse.setSuccess(StringUtils.isBlank(fileWrapper.getMsg()));
        // 告知开启session成功
        RpcMsgTransUtil.write(ctx.channel(), rpcResponse);
        // 没有异常情况
        if (StringUtils.isBlank(fileWrapper.getMsg())) {
            if (!fileWrapper.isNeedTrans()) { // 直接结束
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, fileWrapper.getFile(), fileWrapper.getTransModel(), rpcFileRequest.getFileInfo(), 0L);
                RpcFileReceiverHandlerExecProxy.onSuccess(rpcFileReceiverHandler, impl);
                log.info("receiver file reception ends: No transfer required");
            } else {
                long length = rpcFileRequest.getFileInfo().getLength() - fileWrapper.getWriteIndex();
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, fileWrapper.getFile(), fileWrapper.getTransModel(), rpcFileRequest.getFileInfo(), length);
                RpcSessionTransManger.initFile(rpcSession, NumberConstant.SEVENTY_FIVE, impl, ctx.channel().id().asShortText());
                VirtualThreadPool.execute(() -> handleAsynRecieveFile(ctx, rpcFileRequest, rpcFileReceiverHandler));
            }
        } else {
            log.error("recipient file receipt ends: " + fileWrapper.getMsg());
        }
    }


    @SneakyThrows
    private static void handleAsynRecieveFile(ChannelHandlerContext ctx, final RpcFileRequest rpcFileRequest, final RpcFileReceiverHandler rpcFileReceiverHandler) {
        RpcFileReceiveWrapper impl = (RpcFileReceiveWrapper) RpcSessionTransManger.getContextWrapper(rpcFileRequest.getRpcSession().getSessionId());
        File targetFile = impl.getFile();
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        long length = impl.getNeedTransLength();
        long chunkSize = rpcFileRequest.getBuffer();
        long chunks = (length + chunkSize - 1) / chunkSize;
        RpcResponse response = rpcFileRequest.toResponse();
        boolean isProcessOverride = ReflectUtil.isOverridingInterfaceDefaultMethod(rpcFileReceiverHandler.getClass(), "onProcess");
        try {
            AtomicInteger handleChunks = new AtomicInteger();
            // 以追加模式打开目标文件
            try (FileOutputStream fos = new FileOutputStream(targetFile, true); FileChannel fileChannel = fos.getChannel()) {
                for (int i = 0; i < chunks; i++) {
                    RpcSessionTransManger.FileChunkItem poll = RunnerUtil.tryTimesUntilNotNull(() -> RpcSessionTransManger.isRunning(rpcSession.getSessionId()), 3, () -> RpcSessionTransManger.poll(rpcSession.getSessionId(), rpcSession.getTimeOutMillis() / 3));
                    // 拉取之后也要判断是否正常
                    if (poll == null) {
                        if (!RpcSessionTransManger.isRunning(rpcSession.getSessionId())) {
                            break;
                        }
                        throw new RpcException(RpcErrorEnum.HANDLE_MSG, "file block receive timeout");
                    }
                    if (i != poll.getSerial()) {
                        throw new RpcException(RpcErrorEnum.HANDLE_MSG, "file blocks are missing:" + i);
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
                        RpcFileReceiverHandlerExecProxy.onProcess(rpcFileReceiverHandler, impl, recieveSize);
                    }
                    handleChunks.incrementAndGet();
                }
                if (handleChunks.get() == chunks) {
                    RpcFileReceiverHandlerExecProxy.onSuccess(rpcFileReceiverHandler, impl);
                }
            }
        } catch (Exception e) {
            log.error("file Block - Merge - Print Exception Information", e);
            response.setMsg(e.getMessage());
            response.setSuccess(false);
            RpcMsgTransUtil.write(ctx.channel(), response);
            RpcFileReceiverHandlerExecProxy.onFailure(rpcFileReceiverHandler, impl, e);
        } finally {
            RpcSessionTransManger.release(rpcSession.getSessionId());
        }
    }

    private static void sendStartError(RpcResponse rpcResponse, Channel channel, String message) {
        rpcResponse.setMsg(message);
        rpcResponse.setSuccess(false);
        RpcMsgTransUtil.write(channel, rpcResponse);
    }
}