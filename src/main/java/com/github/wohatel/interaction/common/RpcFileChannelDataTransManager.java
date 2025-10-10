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
            sendStartError(rpcResponse, ctx.channel(), "there is no receive file configuration for remote receive file events: Transmission terminated");
            return;
        }
        if (rpcFileRequest.isSessionStart()) {
            boolean running = RpcSessionTransManger.isRunning(rpcFileRequest.getRpcSession().getSessionId());
            if (running) {
                sendStartError(rpcResponse, ctx.channel(), "do not enable repeat sessions, please check");
                return;
            }
            String body = rpcFileRequest.getBody();
            RpcSessionContext sessionContext = JsonUtil.fromJson(body, RpcSessionContext.class);
            try {
                RpcFileLocal rpcFileWrapper = rpcFileReceiverHandler.getTargetFile(rpcFileRequest.getRpcSession(), sessionContext, rpcFileRequest.getFileInfo());
                if (rpcFileWrapper == null) {
                    sendStartError(rpcResponse, ctx.channel(), "remote accept file path error: send terminated");
                    return;
                }
                readInitFile(ctx, rpcFileRequest, sessionContext, rpcFileWrapper, rpcFileReceiverHandler);
            } catch (Exception e) {
                sendStartError(rpcResponse, ctx.channel(), e.getMessage());
            }
        } else if (rpcFileRequest.isSessionFinish()) {
            RpcSession rpcSession = rpcFileRequest.getRpcSession();
            boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
            if (!running) {
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
        boolean addStatus = RpcSessionTransManger.addFileChunk(rpcSession.getSessionId(), item);
        if (!addStatus) {
            RpcResponse response = rpcFileRequest.toResponse();
            response.setSuccess(false);
            response.setMsg("stop receiving file blocks");
            RpcMsgTransManager.sendResponse(ctx.channel(), response);
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
        RpcMsgTransManager.sendResponse(ctx.channel(), rpcResponse);

        if (StringUtils.isBlank(fileWrapper.getMsg())) {
            if (!fileWrapper.isNeedTrans()) {
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
            try (FileOutputStream fos = new FileOutputStream(targetFile, true); FileChannel fileChannel = fos.getChannel()) {
                for (int i = 0; i < chunks; i++) {
                    RpcSessionTransManger.FileChunkItem poll = RunnerUtil.tryTimesUntilNotNull(() -> RpcSessionTransManger.isRunning(rpcSession.getSessionId()), 3, () -> RpcSessionTransManger.poll(rpcSession.getSessionId(), rpcSession.getTimeOutMillis() / 3));
                    // After pulling, you should also judge whether it is normal
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
                    int readableBytes = byteBuf.readableBytes();
                    // 无需释放,前边解析使用的是unpooled堆内存
                    byteBuf.getBytes(byteBuf.readerIndex(), fileChannel, readableBytes);
                    long receiveSize = i != chunks - 1 ? (i + 1) * chunkSize : length;
                    response.setBody(String.valueOf(receiveSize));
                    RpcMsgTransManager.sendResponse(ctx.channel(), response);
                    if (isProcessOverride) {
                        // 同步执行
                        RpcFileReceiverHandlerExecProxy.onProcess(rpcFileReceiverHandler, impl, receiveSize);
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
            RpcMsgTransManager.sendResponse(ctx.channel(), response);
            RpcFileReceiverHandlerExecProxy.onFailure(rpcFileReceiverHandler, impl, e);
        } finally {
            RpcSessionTransManger.release(rpcSession.getSessionId());
        }
    }

    private static void sendStartError(RpcResponse rpcResponse, Channel channel, String message) {
        rpcResponse.setMsg(message);
        rpcResponse.setSuccess(false);
        RpcMsgTransManager.sendResponse(channel, rpcResponse);
    }
}