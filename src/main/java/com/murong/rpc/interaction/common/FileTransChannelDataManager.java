package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONArray;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileLocalWrapper;
import com.murong.rpc.interaction.file.RpcFileRequest;
import com.murong.rpc.interaction.file.RpcFileWrapperUtil;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.util.JsonUtil;
import com.murong.rpc.util.ReflectUtil;
import com.murong.rpc.util.RunnerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.Triple;

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
    public static void channelRead(Channel channel, RpcMsg rpcMsg, RpcFileRequestHandler rpcFileRequestHandler) {
        RpcFileRequest rpcFileRequest = rpcMsg.getPayload(RpcFileRequest.class);
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        if (rpcFileRequestHandler == null) {
            sendStartError(rpcResponse, channel, "远端接收文件事件暂无接收文件配置:发送终止");
            return;
        }
        if (rpcFileRequest.isSessionStart()) {
            boolean running = TransSessionManger0.isRunning(rpcFileRequest.getRpcSession().getSessionId());
            if (running) {
                sendStartError(rpcResponse, channel, "请勿开启重复session,请检查");
                return;
            }
            String body = rpcFileRequest.getBody();
            RpcSessionContext sessionContext = JsonUtil.fromJson(body, RpcSessionContext.class);
            RpcFileContext context = new RpcFileContext(System.currentTimeMillis(), rpcFileRequest.getLength(), rpcFileRequest.getFileName(), rpcFileRequest.getRpcSession(), sessionContext);
            RpcFileLocalWrapper rpcFileWrapper = rpcFileRequestHandler.getTargetFile(context);
            if (rpcFileWrapper == null) {
                sendStartError(rpcResponse, channel, "远端接受文件路径错误:发送终止");
                return;
            }
            // 继续处理逻辑
            readInitFile(channel, rpcFileRequest, context, rpcFileWrapper, rpcFileRequestHandler);
        } else if (rpcFileRequest.isSessionFinish()) {
            String sessionId = rpcFileRequest.getRpcSession().getSessionId();
            boolean running = TransSessionManger0.isRunning(sessionId);
            if (!running) {// 如果已经不再运行,则无需执行
                return;
            }
            Triple<RpcFileContext, RpcFileLocalWrapper, Channel> data = TransSessionManger0.getFileData(sessionId);
            RpcFileContext fileContext = data.getLeft();
            RpcFileLocalWrapper rpcFileWrapper = data.getMiddle();
            TransSessionManger0.release(sessionId);
            VirtualThreadPool.execute(() -> rpcFileRequestHandler.onStop(fileContext, rpcFileWrapper));
        } else {
            readBodyFile(channel, rpcFileRequest, rpcMsg.getByteBuffer());
        }
    }

    @SneakyThrows
    private static void readBodyFile(Channel channel, RpcFileRequest rpcFileRequest, ByteBuf byteBuf) {
        TransSessionManger0.FileChunkItem item = new TransSessionManger0.FileChunkItem();
        item.setByteBuf(byteBuf);
        item.setBuffer(rpcFileRequest.getBuffer());
        item.setLength(rpcFileRequest.getLength());
        item.setSerial(rpcFileRequest.getSerial());
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        boolean addStatus = TransSessionManger0.addOrReleaseFile(rpcSession.getSessionId(), item);
        if (!addStatus) {
            RpcResponse response = rpcFileRequest.toResponse();
            response.setSuccess(false);
            response.setMsg("停止接收文件块");
            RpcMsgTransUtil.write(channel, response);
        }
    }

    private static void readInitFile(Channel channel, RpcFileRequest rpcFileRequest, RpcFileContext context, RpcFileLocalWrapper fileLocalWrapper, RpcFileRequestHandler rpcFileRequestHandler) {
        RpcFileWrapperUtil fileWrapper = RpcFileWrapperUtil.fromLocalWrapper(fileLocalWrapper);
        fileWrapper.init(rpcFileRequest.getLength());
        List<String> body = new ArrayList<>();
        body.add(String.valueOf(fileWrapper.isNeedTrans()));
        body.add(fileWrapper.getTransModel().name());
        body.add(String.valueOf(fileWrapper.getWriteIndex()));
        body.add(fileWrapper.getFile().getAbsolutePath());
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        rpcResponse.setBody(JSONArray.toJSONString(body));
        rpcResponse.setMsg(fileWrapper.getMsg());
        RpcMsgTransUtil.write(channel, rpcResponse);
        if (fileWrapper.isInterruptByInit()) {
            VirtualThreadPool.execute(() -> rpcFileRequestHandler.onSuccess(context, fileLocalWrapper));
            log.info("接收方文件接收结束: 无需传输");
        } else {
            VirtualThreadPool.execute(fileWrapper.isNeedTrans(), () -> handleAsynRecieveFile(channel, rpcFileRequest, context, fileLocalWrapper, fileWrapper.getWriteIndex(), rpcFileRequestHandler));
        }
    }

    @SneakyThrows
    private static void handleAsynRecieveFile(Channel channel, final RpcFileRequest rpcFileRequest, final RpcFileContext context, final RpcFileLocalWrapper fileWrapper, long index, final RpcFileRequestHandler rpcFileRequestHandler) {
        File targetFile = fileWrapper.getFile();
        String sessionId = rpcFileRequest.getRpcSession().getSessionId();
        long length = rpcFileRequest.getLength() - index;
        long chunkSize = rpcFileRequest.getBuffer();
        long chunks = (length + chunkSize - 1) / chunkSize;
        RpcResponse response = rpcFileRequest.toResponse();
        Triple<RpcFileContext, RpcFileLocalWrapper, Channel> triple = Triple.of(context, fileWrapper, channel);
        TransSessionManger0.initFile(sessionId, NumberConstant.SEVENTY_FIVE, triple, rpcFileRequest.getRpcSession());
        boolean isProcessOverride = ReflectUtil.isOverridingInterfaceDefaultMethod(rpcFileRequestHandler.getClass(), "onProcess");
        try {
            AtomicInteger handleChunks = new AtomicInteger();
            // 以追加模式打开目标文件
            try (FileOutputStream fos = new FileOutputStream(targetFile, true); FileChannel fileChannel = fos.getChannel()) {
                for (int i = 0; i < chunks; i++) {
                    TransSessionManger0.FileChunkItem poll = RunnerUtil.tryTimesUntilNotNull(() -> TransSessionManger0.isRunning(sessionId), 3, () -> TransSessionManger0.poll(sessionId, context.getRpcSession().getTimeOutMillis() / 3));
                    // 拉取之后也要判断是否正常
                    if (poll == null) {
                        if (!TransSessionManger0.isRunning(sessionId)) {
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
                    RpcMsgTransUtil.write(channel, response);
                    if (isProcessOverride) {
                        // 同步执行
                        RunnerUtil.execSilent(() -> rpcFileRequestHandler.onProcess(context, fileWrapper, recieveSize));
                    }
                    handleChunks.incrementAndGet();
                }
                if (handleChunks.get() == chunks) {
                    RunnerUtil.execSilent(() -> rpcFileRequestHandler.onSuccess(context, fileWrapper));
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "异常", e);
            response.setMsg(e.getMessage());
            response.setSuccess(false);
            RpcMsgTransUtil.write(channel, response);
            RunnerUtil.execSilent(() -> rpcFileRequestHandler.onFailure(context, fileWrapper, e));
        } finally {
            TransSessionManger0.release(sessionId);
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