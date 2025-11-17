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
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.interaction.file.RpcFileSignature;
import com.github.wohatel.interaction.file.RpcFileSignatureRotary;
import com.github.wohatel.interaction.handler.RpcFileRequestMsgHandler;
import com.github.wohatel.util.DefaultVirtualThreadPool;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.ReflectUtil;
import com.github.wohatel.util.RunnerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
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
 * RpcFileChannelDataTransProxy is a proxy class that handles file data transmission through RPC channels.
 * leChannelDataTransProxy is a proxy class that handles file data transmission through RPC channels.
 * It provides methods to read file data, handle different session processes, and manage file transfers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class RpcFileChannelDataTransProxy {

    /**
     * Handles channel read operations for file data transmission.
     *
     * @param ctx                      The channel handler context
     * @param rpcMsg                   The RPC message containing file request data
     * @param rpcFileRequestMsgHandler The handler for file request messages
     * @throws SneakyThrows if an exception occurs during execution
     */
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


    /*
     *  Reads and processes a file body chunk from the network channel.
     * This method handles file transmission by adding received chunks to the transmission manager.
     *
     * @param ctx The ChannelHandlerContext which contains the connection information
     * @param rpcFileRequest The request object containing file transmission metadata
     * @param byteBuf The ByteBuf containing the actual file data chunk
     */
    @SneakyThrows
    private static void readBodyFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, ByteBuf byteBuf) {
        // Create a new FileChunkItem to store the received file chunk
        RpcSessionTransManger.FileChunkItem item = new RpcSessionTransManger.FileChunkItem();
        item.setByteBuf(byteBuf);           // Set the actual data buffer
        item.setBuffer(rpcFileRequest.getBlockSize());  // Set the expected block size
        item.setSerial(rpcFileRequest.getSerial());     // Set the chunk sequence number
        // Get the RPC session from the request
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        // Add the file chunk to the transmission manager
        boolean addStatus = RpcSessionTransManger.addFileChunk(rpcSession.getSessionId(), item);
        // If adding the chunk failed, send an error reaction
        if (!addStatus) {
            RpcReaction reaction = rpcFileRequest.toReaction();
            reaction.setSuccess(false);
            reaction.setMsg("stop receiving file blocks");
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    /**
     *  Processes file signature rotation and sends reaction response back to client
     *
     * @param ctx           The channel handler context for the current connection
     * @param rpcFileRequest The original file request received from client
     * @param signature     The file signature information containing security details
     * @param fileInfo      The file information including length and path details
     * @return              RpcFileSignatureRotaryResult containing rotation operation results
     */
    private static RpcFileSignatureRotary.RpcFileSignatureRotaryResult rotaryAndReaction(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcFileSignature signature, RpcFileInfo fileInfo) {
        // Create rotary instance from signature wrapper and perform rotation operation
        RpcFileSignatureRotary fileSignatureRotary = RpcFileSignatureRotary.fromLocalWrapper(signature);
        RpcFileSignatureRotary.RpcFileSignatureRotaryResult rotaryResult = fileSignatureRotary.rotary(fileInfo.getLength());
        // Prepare response body components as list of strings
        List<String> body = new ArrayList<>();
        body.add(String.valueOf(rotaryResult.isNeedTrans()));      // Whether file transfer is needed
        body.add(signature.getTransModel().name());
        body.add(String.valueOf(rotaryResult.getWriteIndex()));
        body.add(signature.getFile().getAbsolutePath());                // Transfer model name
        // Write index for file transfer
        RpcReaction rpcReaction = rpcFileRequest.toReaction();            // Absolute file path
        rpcReaction.setBody(JSONArray.toJSONString(body));
        // Create and configure reaction response
        rpcReaction.setMsg(rotaryResult.getMsg());
        rpcReaction.setSuccess(StringUtils.isBlank(rotaryResult.getMsg()));         // Set JSON formatted body
        RpcMsgTransManager.sendReaction(ctx.channel(), rpcReaction);                  // Set operation message
        return rotaryResult;  // Set success status based on message
        // Send reaction back to client
    }

    /**
     * Reads and processes initialization file from RPC request
     *
     * @param ctx                      The channel handler context for the RPC communication
     * @param rpcFileRequest           The RPC file request containing file transfer details
     * @param context                  The RPC session context maintaining session state
     * @param signature                The RPC file signature containing file metadata and transfer parameters
     * @param rpcFileRequestMsgHandler The handler for processing file request messages
     * @param fileInfo                 Information about the file being transferred
     */
    private static void readInitFile(ChannelHandlerContext ctx, RpcFileRequest rpcFileRequest, RpcSessionContext context, RpcFileSignature signature, RpcFileRequestMsgHandler rpcFileRequestMsgHandler, RpcFileInfo fileInfo) {
        // Perform file rotation and reaction checks
        RpcFileSignatureRotary.RpcFileSignatureRotaryResult rotaryResult = rotaryAndReaction(ctx, rpcFileRequest, signature, fileInfo);
        // Get RPC session from the request
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        // Check if rotation was successful
        if (rotaryResult.isSuccess()) {
            // Case 1: No transfer required after rotation
            if (!rotaryResult.isNeedTrans()) {
                // Create file receive wrapper with zero length (no transfer)
                // Notify handler of successful completion
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, signature.getFile(), signature.getTransModel(), fileInfo, 0L);
                // Execute cleanup operations
                RpcFileRequestMsgHandlerExecProxy.onSuccess(rpcFileRequestMsgHandler, impl);
                RpcFileRequestMsgHandlerExecProxy.onFinally(rpcFileRequestMsgHandler, impl);
                log.info("receiver file reception ends: No transfer required");
            } else {
                long length = fileInfo.getLength() - rotaryResult.getWriteIndex();
                RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, signature.getFile(), signature.getTransModel(), fileInfo, length);
                RpcSessionTransManger.initFile(rpcSession, RpcNumberConstant.SEVENTY_FIVE, impl);
                RpcSessionTransManger.registOnRelease(rpcSession.getSessionId(), t -> RpcFileRequestMsgHandlerExecProxy.onFinally(rpcFileRequestMsgHandler, impl));
                DefaultVirtualThreadPool.execute(() -> handleAsynReceiveFile(ctx, rpcFileRequest, rpcFileRequestMsgHandler));
            }
        } else {
            log.error("recipient file receipt ends: {}", rotaryResult.getMsg());
            RpcFileReceiveWrapper impl = new RpcFileReceiveWrapper(rpcSession, context, signature.getFile(), signature.getTransModel(), fileInfo, 0L);
            RpcFileRequestMsgHandlerExecProxy.onFailure(rpcFileRequestMsgHandler, impl, new RpcException(rotaryResult.getMsg()));
            RpcFileRequestMsgHandlerExecProxy.onFinally(rpcFileRequestMsgHandler, impl);
        }
    }


    /**
     * Handles asynchronous file reception with proper error handling and progress reporting
     *
     * @param ctx                      The channel handler context for communication
     * @param rpcFileRequest           The request containing file transfer details
     * @param rpcFileRequestMsgHandler The handler for file transfer events
     * @throws SneakyThrows To propagate exceptions without explicit declaration
     */
    @SneakyThrows
    private static void handleAsynReceiveFile(ChannelHandlerContext ctx, final RpcFileRequest rpcFileRequest, final RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        RpcFileReceiveWrapper impl = (RpcFileReceiveWrapper) RpcSessionTransManger.getContextWrapper(rpcFileRequest.getRpcSession().getSessionId());
        File targetFile = impl.getFile();
        RpcSession rpcSession = rpcFileRequest.getRpcSession();
        long length = impl.getNeedTransLength();
        long chunkSize = rpcFileRequest.getBlockSize();
        long chunks = (length + chunkSize - 1) / chunkSize;
        RpcReaction reaction = rpcFileRequest.toReaction();
        boolean isProcessOverride = ReflectUtil.isOverridingInterfaceDefaultMethod(rpcFileRequestMsgHandler.getClass(), "onProcess");
        try {
            AtomicInteger handleChunks = new AtomicInteger();
            RpcFileInterrupter rpcFileInterrupter = new RpcFileInterrupter(rpcSession.getSessionId());
            long waitTime = Math.round(rpcSession.getTimeOutMillis() / 4.0);
            try (FileOutputStream fos = new FileOutputStream(targetFile, true); FileChannel fileChannel = fos.getChannel()) { // Calculate wait time
                // Process each chunk
                for (int i = 0; i < chunks; i++) {
                    RpcSessionTransManger.FileChunkItem poll = RunnerUtil.tryTimesUntilNotNull(() -> RpcSessionTransManger.isRunning(rpcSession.getSessionId()), 3, () -> RpcSessionTransManger.poll(rpcSession.getSessionId(), waitTime));
                    // Try to get file chunk with timeout handling
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

    /**
     * Handles the start of a file transfer request by processing the RPC request and setting up the necessary session context.
     *
     * @param ctx                      The ChannelHandlerContext of the current channel
     * @param request                  The RpcFileRequest containing the file transfer details
     * @param rpcFileRequestMsgHandler The handler for processing RPC file request messages
     */
    public static void handleToStart(ChannelHandlerContext ctx, RpcFileRequest request, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        // Convert the request to a reaction object
        RpcReaction reaction = request.toReaction();
        // Check if there's already a running session with the same ID
        boolean running = RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId());
        if (running) {
            // Set error reaction for duplicate session
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setMsg("repeat session already running");
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
            return;
        }
        // Deserialize the session context from request body
        RpcSessionContext sessionContext = JsonUtil.fromJson(request.getBody(), RpcSessionContext.class);
        // Deserialize the file info from request header
        RpcFileInfo rpcFileInfo = JsonUtil.fromJson(request.getHeader(), RpcFileInfo.class);
        try {
            // Get the target file signature using the request handler
            RpcFileSignature signature = rpcFileRequestMsgHandler.getTargetFile(request.getRpcSession(), sessionContext, rpcFileInfo);
            if (signature == null) {
                // Handle case where signature is null
                reaction.setSuccess(false);
                reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
                reaction.setMsg("remote accept file error: signature is null");
                RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                return;
            }
            if (!signature.isAgreed()) {
                // Handle case where signature agreement is not given
                reaction.setSuccess(false);
                reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
                reaction.setMsg(signature.getMsg());
                RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
                return;
            }
            // Initialize file reading if all checks pass
            readInitFile(ctx, request, sessionContext, signature, rpcFileRequestMsgHandler, rpcFileInfo);
        } catch (Exception e) {
            // Handle any exceptions during the process
            reaction.setSuccess(false);
            reaction.setCode(RpcErrorEnum.HANDLE_MSG.getCode());
            reaction.setMsg(e.getMessage());
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        }
    }

    /**
     * Handles the processing of running RPC file transfer requests.
     * This method checks if the session is still active and either processes the file transfer
     * or sends an error response if the session has been lost.
     *
     * @param ctx     The ChannelHandlerContext which contains the channel information
     * @param request The RpcFileRequest containing the file transfer request details
     * @param byteBuf The ByteBuf containing the file data to be processed
     */
    public static void handleRunning(ChannelHandlerContext ctx, RpcFileRequest request, ByteBuf byteBuf) {
        // Check if the session is still running
        if (!RpcSessionTransManger.isRunning(request.getRpcSession().getSessionId())) {
            // Create a reaction response for the lost session scenario
            RpcReaction reaction = request.toReaction();
            // Set error message with request ID for tracking
            reaction.setMsg("{requestId:" + request.getRequestId() + "} the sending session file message is abnormal and the session does not exist");
            // Mark the reaction as unsuccessful
            reaction.setSuccess(false);
            // Set the error code for session loss
            reaction.setCode(RpcErrorEnum.SESSION_LOSE.getCode());
            // Send the error reaction back to the client
            RpcMsgTransManager.sendReaction(ctx.channel(), reaction);
        } else {
            // If session is valid, proceed with reading the file body
            readBodyFile(ctx, request, byteBuf);
        }
    }

    /**
     * Handles the completion of a file transfer request in the RPC system.
     * This method checks if the session is still active and releases resources if needed.
     *
     * @param ctx                      The ChannelHandlerContext which provides various contextual information and operations
     * @param request                  The RpcFileRequest containing details about the file transfer
     * @param rpcFileRequestMsgHandler The handler for RPC file request messages
     */
    public static void handleFinished(ChannelHandlerContext ctx, RpcFileRequest request, RpcFileRequestMsgHandler rpcFileRequestMsgHandler) {
        // Get the RPC session from the request
        RpcSession rpcSession = request.getRpcSession();
        // Check if the session is still running
        boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
        if (!running) {
            // If not running, return without taking any action
            return;
        }
        // Release the session resources if it was running
        RpcSessionTransManger.release(rpcSession.getSessionId());
    }
}