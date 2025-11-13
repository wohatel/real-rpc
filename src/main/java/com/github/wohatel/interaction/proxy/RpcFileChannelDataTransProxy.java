package com.github.wohatel.interaction.proxy;

import com.alibaba.fastjson2.JSONArray;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcReaction;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.common.RpcFileInterrupter;
import com.github.wohatel.interaction.common.RpcMsgTransManager;
import com.github.wohatel.interaction.common.RpcSessionContext;
import com.github.wohatel.interaction.common.RpcSessionTransManger;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.interaction.file.RpcFileSignature;
import com.github.wohatel.interaction.file.RpcFileSignatureRotary;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.ReflectUtil;
import com.github.wohatel.util.RunnerUtil;
import com.github.wohatel.util.VirtualThreadPool;
import io.netty.buffer.ByteBuf;
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
public class RpcFileChannelDataTransProxy {

    @SneakyThrows
    public static void channelRead(ChannelHandlerContext ctx, RpcMsg rpcMsg, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcFileRequest rpcFileRequest = rpcMsg.getPayload(RpcFileRequest.class);
        if (rpcFileRequestMsgHandler == null) {
            RpcReaction reaction = rpcFileRequest.toReaction();
            String errorMsg = "remote endpoint has no file request handler";
            reaction.setMsg(errorMsg);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setSuccess(false);
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            return;
        }
        RpcSessionProcess sessionProcess = rpcFileRequest.getSessionProcess();
        switch (sessionProcess) {
            case TOSTART -> handleToStart(ctx, rpcFileRequest, rpcFileRequestMsgHandler);
            case RUNNING -> handleRunning(ctx, rpcFileRequest, rpcMsg.getByteBuffer());
            case FINISHED -> handleFinished(ctx, rpcFileRequest, rpcFileRequestMsgHandler);
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
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    /**
     * 判决文件协议
     *
     * @param ctx            通道
     * @param rpcFileRequest 请求
     * @param signature      协议
     * @return RpcFileSignatureRotaryResult 判决
     */
    private static RpcFileSignatureRotary.RpcFileSignatureRotaryResult rotaryAndReaction(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcFileSignature signature) {
        RpcFileSignatureRotary fileWrapper = RpcFileSignatureRotary.fromLocalWrapper(signature);
        RpcFileSignatureRotary.RpcFileSignatureRotaryResult rotaryResult = fileWrapper.rotary(rpcFileRequest.getFileInfo().getLength());
        List<String> body = new ArrayList<>();
        body.add(String.valueOf(rotaryResult.isNeedTrans()));
        body.add(signature.getTransModel().name());
        body.add(String.valueOf(rotaryResult.getWriteIndex()));
        body.add(signature.getFile().getAbsolutePath());

        RpcReaction rpcReaction = rpcFileRequest.toReaction();
        rpcReaction.setBody(JSONArray.toJSONString(body));
        rpcReaction.setMsg(rotaryResult.getMsg());
        rpcReaction.setSuccess(StringUtils.isBlank(rotaryResult.getMsg()));
        RpcMsgTransManager.sendReaction(ctx.channel(), rpcReaction);
        return rotaryResult;
    }

    /**
     * 初始化文件的接收
     *
     * @param ctx                      通道
     * @param rpcFileRequest           请求
     * @param context                  上下文
     * @param signature                协议
     * @param rpcFileRequestMsgHandler 文件请求handler
     */
    private static void readInitFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcSessionContext context, RpcFileSignature signature, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcFileSignatureRotary.RpcFileSignatureRotaryResult rotaryResult = rotaryAndReaction(ctx, rpcFileRequest, signature);
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        if (rotaryResult.isAgreed()) {
            if (!rotaryResult.isNeedTrans()) {
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, signature.getFile(), signature.getTransModel(), rpcFileRequest.getFileInfo(), 0L);
                RpcFileRequestMsgHandlerExecProxy.onSuccess(rpcFileRequestMsgHandler, impl);
                RpcFileRequestMsgHandlerExecProxy.onFinally(rpcFileRequestMsgHandler, impl);
                log.info("receiver file reception ends: No transfer required");
            } else {
                long length = rpcFileRequest.getFileInfo().getLength() - rotaryResult.getWriteIndex();
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, signature.getFile(), signature.getTransModel(), rpcFileRequest.getFileInfo(), length);
                RpcSessionTransManger.initFile(rpcSession, NumberConstant.SEVENTY_FIVE, impl);
                RpcSessionTransManger.registOnRelease(rpcSession.getSessionId(), t -> RpcFileRequestMsgHandlerExecProxy.onFinally(rpcFileRequestMsgHandler, impl));
                VirtualThreadPool.execute(() -> handleAsynReceiveFile(ctx, rpcFileRequest, rpcFileRequestMsgHandler));
            }
        } else {
            log.error("recipient file receipt ends: {}", rotaryResult.getMsg());
            RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, signature.getFile(), signature.getTransModel(), rpcFileRequest.getFileInfo(), 0L);
            RpcFileRequestMsgHandlerExecProxy.onFailure(rpcFileRequestMsgHandler, impl, new RpcException(rotaryResult.getMsg()));
            RpcFileRequestMsgHandlerExecProxy.onFinally(rpcFileRequestMsgHandler, impl);
        }
    }

    /**
     * 异步接收文件
     *
     * @param ctx                      通道
     * @param rpcFileRequest           请求
     * @param rpcFileRequestMsgHandler 文件处理handler
     */
    @SneakyThrows
    private static void handleAsynReceiveFile(ChannelHandlerContext ctx, final RpcFileRequest rpcFileRequest, final RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
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
            RpcFileInterrupter rpcFileInterrupter = new RpcFileInterrupter(rpcSession.getSessionId());
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
                    long receivedSize = i != chunks - 1 ? (i + 1) * chunkSize : length;
                    reaction.setBody(String.valueOf(receivedSize));
                    RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                    if (isProcessOverride) {
                        // 同步执行
                        RpcFileRequestMsgHandlerExecProxy.onProcess(rpcFileRequestMsgHandler, impl, receivedSize, rpcFileInterrupter);
                    }
                    handleChunks.incrementAndGet();
                }
                if (handleChunks.get() == chunks) {
                    RpcFileRequestMsgHandlerExecProxy.onSuccess(rpcFileRequestMsgHandler, impl);
                }
            }
        } catch (Exception e) {
            log.error("file Block - Merge - Print Exception Information", e);
            reaction.setMsg(e.getMessage());
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            RpcFileRequestMsgHandlerExecProxy.onFailure(rpcFileRequestMsgHandler, impl, e);
        } finally {
            RpcSessionTransManger.release(rpcSession.getSessionId());
        }
    }

    public static void handleToStart(ChannelHandlerContext ctx, RpcFileRequest request, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcReaction reaction = request.toReaction();
        boolean running = RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId());
        if (running) {
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setMsg("repeat session already running");
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            return;
        }
        RpcSessionContext sessionContext = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
        try {
            RpcFileSignature signature = rpcFileRequestMsgHandler.getTargetFile(request.getRpcSession(), sessionContext, request.getFileInfo());
            if (signature == null) {
                reaction.setSuccess(false);
                reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
                reaction.setMsg("remote accept file path error: send terminated");
                RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                return;
            }
            if (!signature.isAgreed()) {
                reaction.setSuccess(false);
                reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
                reaction.setMsg(signature.getMsg());
                RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                return;
            }
            readInitFile(ctx, request, sessionContext, signature, rpcFileRequestMsgHandler);
        } catch (Exception e) {
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setMsg(e.getMessage());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    public static void handleRunning(ChannelHandlerContext ctx, RpcFileRequest request, ByteBuf byteBuf) {
        if (!RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId())) {
            RpcReaction reaction = request.toReaction();
            reaction.setMsg("{requestId:" + request.getRequestId() + "} the sending session file message is abnormal and the session does not exist");
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.SESSION_LOSE.getCode());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        } else {
            readBodyFile(ctx, request, byteBuf);
        }
    }

    public static void handleFinished(ChannelHandlerContext ctx, RpcFileRequest request, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcSession rpcSession = request.getRpcSession();
        boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
        if (!running) {
            return;
        }
        RpcSessionTransManger.release(rpcSession.getSessionId());
    }
}