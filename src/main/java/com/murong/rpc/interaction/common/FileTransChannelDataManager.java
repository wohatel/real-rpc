package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionContext;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileRequest;
import com.murong.rpc.interaction.file.RpcFileTransInterrupter;
import com.murong.rpc.interaction.file.RpcFileWrapper;
import com.murong.rpc.interaction.handler.RpcFileRequestHandler;
import com.murong.rpc.util.ReflectUtil;
import com.murong.rpc.util.RunnerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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

@Log
public class FileTransChannelDataManager {

    @SneakyThrows
    public static void channelRead(Channel channel, RpcMsg rpcMsg, RpcFileRequestHandler rpcFileRequestHandler) {
        RpcFileRequest rpcFileRequest = rpcMsg.getPayload(RpcFileRequest.class);
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        if (rpcFileRequestHandler == null) {
            List<String> rbody = new ArrayList<>();
            rbody.add(String.valueOf(false));
            rbody.add(null);
            rbody.add(String.valueOf(0));
            rbody.add("远端接收文件事件暂无接收文件配置:发送终止");
            rpcResponse.setBody(JSONArray.toJSONString(rbody));
            RpcMsgTransUtil.write(channel, rpcResponse);
            return;
        }
        if (rpcFileRequest.isSessionStart()) {
            RpcSessionContext rpcSessionContext = JSONObject.parseObject(rpcFileRequest.getBody(), RpcSessionContext.class);
            RpcFileContext context = new RpcFileContext(System.currentTimeMillis(), rpcFileRequest.getLength(), rpcFileRequest.getRpcSession().getSessionId(), rpcFileRequest.getFileName(), rpcSessionContext, rpcFileRequest.getChunkHandleTimeOut());
            RpcFileWrapper rpcFileWrapper = rpcFileRequestHandler.getTargetFile(context);
            if (rpcFileWrapper == null) {
                List<String> rbody = new ArrayList<>();
                rbody.add(String.valueOf(false));
                rbody.add(null);
                rbody.add(String.valueOf(0));
                rbody.add("远端接受文件路径错误:发送终止");
                rpcResponse.setBody(JSONArray.toJSONString(rbody));
                RpcMsgTransUtil.write(channel, rpcResponse);
                return;
            }
            // 将上下文存入toContext
            RpcSessionFuture sessionFuture = RpcInteractionContainer.getSessionFuture(rpcFileRequest.getRpcSession().getSessionId());
            JSONArray toContext = sessionFuture.getContext();
            toContext.set(0, context);
            toContext.set(1, rpcFileWrapper);
            // 继续处理逻辑
            readInitFile(channel, rpcFileRequest, context, rpcFileWrapper, rpcFileRequestHandler);
        } else if (rpcFileRequest.isSessionFinish()) {
            String sessionId = rpcFileRequest.getRpcSession().getSessionId();
            RpcSessionFuture sessionFuture = (RpcSessionFuture) RpcInteractionContainer.remove(sessionId);
            if (sessionFuture == null) {
                log.warning("会话已结束:" + sessionId);
                return;
            }
            JSONArray context = sessionFuture.getContext();
            RpcFileContext fileContext = (RpcFileContext) context.getFirst();
            RpcFileWrapper rpcFileWrapper = (RpcFileWrapper) context.get(1);
            FileTransSessionManger.release(sessionId);
            VirtualThreadPool.execute(() -> rpcFileRequestHandler.onStop(fileContext, rpcFileWrapper));
        } else {
            readBodyFile(channel, rpcFileRequest, rpcMsg.getByteBuffer());
        }
    }

    @SneakyThrows
    private static void readBodyFile(Channel channel, RpcFileRequest rpcFileRequest, ByteBuf byteBuf) {
        FileTransSessionManger.FileChunkItem item = new FileTransSessionManger.FileChunkItem();
        item.setByteBuf(byteBuf);
        item.setBuffer(rpcFileRequest.getBuffer());
        item.setLength(rpcFileRequest.getLength());
        item.setSerial(rpcFileRequest.getSerial());
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        boolean addStatus = FileTransSessionManger.addOrRelease(rpcSession.getSessionId(), item);
        if (!addStatus) {
            RpcResponse response = rpcFileRequest.toResponse();
            response.setSuccess(false);
            response.setMsg("停止接收文件块");
            RpcMsgTransUtil.write(channel, response);
        }
    }

    private static void readInitFile(Channel channel, RpcFileRequest rpcFileRequest, RpcFileContext context, RpcFileWrapper fileWrapper, RpcFileRequestHandler rpcFileRequestHandler) {
        fileWrapper.init(rpcFileRequest.getLength());
        List<String> body = new ArrayList<>();
        body.add(String.valueOf(fileWrapper.isNeedTrans()));
        body.add(fileWrapper.getTransModel().name());
        body.add(String.valueOf(fileWrapper.getWriteIndex()));
        body.add(fileWrapper.getMsg());
        RpcResponse rpcResponse = rpcFileRequest.toResponse();
        rpcResponse.setBody(JSONArray.toJSONString(body));
        RpcMsgTransUtil.write(channel, rpcResponse);
        if (fileWrapper.isInterruptByInit()) {
            VirtualThreadPool.execute(() -> rpcFileRequestHandler.onSuccess(context, fileWrapper));
            log.info("接收方文件接收结束: 无需传输");
        } else {
            VirtualThreadPool.execute(fileWrapper.isNeedTrans(), () -> handleAsynRecieveFile(channel, rpcFileRequest, context, fileWrapper, rpcFileRequestHandler));
        }
    }

    @SneakyThrows
    private static void handleAsynRecieveFile(Channel channel, final RpcFileRequest rpcFileRequest, final RpcFileContext context, final RpcFileWrapper fileWrapper, final RpcFileRequestHandler rpcFileRequestHandler) {
        File targetFile = fileWrapper.getFile();
        String sessionId = rpcFileRequest.getRpcSession().getSessionId();
        long length = rpcFileRequest.getLength() - fileWrapper.getWriteIndex();
        long chunkSize = rpcFileRequest.getBuffer();
        long chunks = (length + chunkSize - 1) / chunkSize;
        RpcResponse response = rpcFileRequest.toResponse();
        FileTransSessionManger.init(sessionId);
        boolean isProcessOverride = ReflectUtil.isOverridingInterfaceDefaultMethod(rpcFileRequestHandler.getClass(), "onProcess");
        try {
            AtomicInteger handleChunks = new AtomicInteger();
            final RpcFileTransInterrupter interrupter = new RpcFileTransInterrupter(channel, context.getSessionId());
            // 以追加模式打开目标文件
            try (FileOutputStream fos = new FileOutputStream(targetFile, true); FileChannel fileChannel = fos.getChannel()) {
                for (int i = 0; i < chunks; i++) {
                    FileTransSessionManger.FileChunkItem poll = RunnerUtil.tryTimesUntilNotNull(() -> FileTransSessionManger.isNormal(sessionId), 3, () -> FileTransSessionManger.poll(sessionId, context.getChunkHandleTimeOut() / 3));
                    // 拉取之后也要判断是否正常
                    if (poll == null) {
                        if (!FileTransSessionManger.isNormal(sessionId)) {
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
                    VirtualThreadPool.execute(isProcessOverride, () -> rpcFileRequestHandler.onProcess(context, fileWrapper, recieveSize, interrupter));
                    handleChunks.incrementAndGet();
                }
                // 如果结束了,则会认为处理完毕所有的块
                VirtualThreadPool.execute(handleChunks.get() == chunks, () -> rpcFileRequestHandler.onSuccess(context, fileWrapper));
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "异常", e);
            response.setMsg(e.getMessage());
            response.setSuccess(false);
            RpcMsgTransUtil.write(channel, response);
            VirtualThreadPool.execute(() -> rpcFileRequestHandler.onFailure(context, fileWrapper, e));
        } finally {
            FileTransSessionManger.release(sessionId);
        }
    }

}