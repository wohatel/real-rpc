package com.github.wohatel.interaction.common;

import com.alibaba.fastjson2.JSONArray;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileLocal;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.interaction.file.RpcFileWrapperUtil;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
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
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcFileRequest rpcFileRequest = rpcMsg.getPayload(RpcFileRequest.class);
        RpcReaction rpcReaction = rpcFileRequest.toReaction();
        if (rpcFileRequestMsgHandler == null) {
            sendStartError(rpcReaction, ctx.channel(), "there is no receive file configuration for remote receive file events: Transmission terminated");
            return;
        }
        if (rpcFileRequest.isSessionStart()) {
            boolean running = RpcSessionTransManger.isRunning(rpcFileRequest.getRpcSession().getSessionId());
            if (running) {
                sendStartError(rpcReaction, ctx.channel(), "do not enable repeat sessions, please check");
                return;
            }
            String body = rpcFileRequest.getBody();
            RpcSessionContext sessionContext = JsonUtil.fromJson(body, RpcSessionContext.class);
            try {
                RpcFileLocal rpcFileWrapper = rpcFileRequestMsgHandler.getTargetFile(rpcFileRequest.getRpcSession(), sessionContext, rpcFileRequest.getFileInfo());
                if (rpcFileWrapper == null) {
                    sendStartError(rpcReaction, ctx.channel(), "remote accept file path error: send terminated");
                    return;
                }
                readInitFile(ctx, rpcFileRequest, sessionContext, rpcFileWrapper, rpcFileRequestMsgHandler);
            } catch (Exception e) {
                sendStartError(rpcReaction, ctx.channel(), e.getMessage());
            }
        } else if (rpcFileRequest.isSessionFinish()) {
            RpcSession rpcSession = rpcFileRequest.getRpcSession();
            boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
            if (!running) {
                return;
            }
            RpcSessionTransManger.release(rpcSession.getSessionId());
        } else {
            if (!RpcSessionTransManger.isRunning(rpcFileRequest.getRpcSession().getSessionId())) {
                RpcReaction reaction = rpcFileRequest.toReaction();
                reaction.setMsg("{requestId:" + rpcFileRequest.getRequestId() + "} the sending session file message is abnormal and the session does not exist");
                reaction.setSuccess(false);
                reaction.setCode(RpcErrorEnum.SESSION_LOSE.getCode());
                RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            } else {
                readBodyFile(ctx, rpcFileRequest, rpcMsg.getByteBuffer());
            }
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
            RpcReaction reaction = rpcFileRequest.toReaction();
            reaction.setSuccess(false);
            reaction.setMsg("stop receiving file blocks");
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    private static void readInitFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcSessionContext context, RpcFileLocal fileLocalWrapper, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        RpcFileWrapperUtil fileWrapper = RpcFileWrapperUtil.fromLocalWrapper(fileLocalWrapper);
        fileWrapper.init(rpcFileRequest.getFileInfo().getLength());
        List<String> body = new ArrayList<>();
        body.add(String.valueOf(fileWrapper.isNeedTrans()));
        body.add(fileWrapper.getTransModel().name());
        body.add(String.valueOf(fileWrapper.getWriteIndex()));
        body.add(fileWrapper.getFile().getAbsolutePath());

        RpcReaction rpcReaction = rpcFileRequest.toReaction();
        rpcReaction.setBody(JSONArray.toJSONString(body));
        rpcReaction.setMsg(fileWrapper.getMsg());
        rpcReaction.setSuccess(StringUtils.isBlank(fileWrapper.getMsg()));
        RpcMsgTransManager.sendReaction(ctx.channel(), rpcReaction);

        if (StringUtils.isBlank(fileWrapper.getMsg())) {
            if (!fileWrapper.isNeedTrans()) {
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, fileWrapper.getFile(), fileWrapper.getTransModel(), rpcFileRequest.getFileInfo(), 0L);
                RpcFileReceiverHandlerExecProxy.onSuccess(rpcFileRequestMsgHandler, impl);
                log.info("receiver file reception ends: No transfer required");
            } else {
                long length = rpcFileRequest.getFileInfo().getLength() - fileWrapper.getWriteIndex();
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, fileWrapper.getFile(), fileWrapper.getTransModel(), rpcFileRequest.getFileInfo(), length);
                RpcSessionTransManger.initFile(rpcSession, NumberConstant.SEVENTY_FIVE, impl, ctx.channel().id().asShortText());
                VirtualThreadPool.execute(() -> handleAsynRecieveFile(ctx, rpcFileRequest, rpcFileRequestMsgHandler));
            }
        } else {
            log.error("recipient file receipt ends: {}", fileWrapper.getMsg());
        }
    }


    @SneakyThrows
    private static void handleAsynRecieveFile(ChannelHandlerContext ctx, final RpcFileRequest rpcFileRequest, final RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcFileReceiveWrapper impl = (RpcFileReceiveWrapper) RpcSessionTransManger.getContextWrapper(rpcFileRequest.getRpcSession().getSessionId());
        File targetFile = impl.getFile();
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        long length = impl.getNeedTransLength();
        long chunkSize = rpcFileRequest.getBuffer();
        long chunks = (length + chunkSize - 1) / chunkSize;
        RpcReaction reaction = rpcFileRequest.toReaction();
        boolean isProcessOverride = ReflectUtil.isOverridingInterfaceDefaultMethod(rpcFileRequestMsgHandler.getClass(), "onProcess");
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
                    reaction.setBody(String.valueOf(receiveSize));
                    RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                    if (isProcessOverride) {
                        // 同步执行
                        RpcFileReceiverHandlerExecProxy.onProcess(rpcFileRequestMsgHandler, impl, receiveSize);
                    }
                    handleChunks.incrementAndGet();
                }
                if (handleChunks.get() == chunks) {
                    RpcFileReceiverHandlerExecProxy.onSuccess(rpcFileRequestMsgHandler, impl);
                }
            }
        } catch (Exception e) {
            log.error("file Block - Merge - Print Exception Information", e);
            reaction.setMsg(e.getMessage());
            reaction.setSuccess(false);
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            RpcFileReceiverHandlerExecProxy.onFailure(rpcFileRequestMsgHandler, impl, e);
        } finally {
            RpcSessionTransManger.release(rpcSession.getSessionId());
        }
    }

    private static void sendStartError(RpcReaction rpcReaction, Channel channel, String message) {
        rpcReaction.setMsg(message);
        rpcReaction.setSuccess(false);
        RpcMsgTransManager.sendReaction(channel, rpcReaction);
    }
}