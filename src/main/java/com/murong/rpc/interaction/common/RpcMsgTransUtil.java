package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONObject;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionProcess;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.file.RpcFileRequest;
import com.murong.rpc.interaction.file.RpcFileTransConfig;
import com.murong.rpc.interaction.file.RpcFileTransProcess;
import com.murong.rpc.interaction.handler.RpcFileTransHandler;
import com.murong.rpc.util.FileUtil;
import com.murong.rpc.util.ReflectUtil;
import com.murong.rpc.util.RpcSpeedLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.UUID;

@Log
public class RpcMsgTransUtil {

    private RpcMsgTransUtil() {
    }

    public static void write(Channel channel, RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            return;
        }
        channel.writeAndFlush(RpcMsg.fromResponse(rpcResponse));
    }

    public static void sendMsg(Channel channel, RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new RuntimeException("连接不可用");
        }
        channel.writeAndFlush(RpcMsg.fromRequest(rpcRequest));
    }

    public static void sendFileMsg(Channel channel, RpcFileRequest rpcRequest, boolean needCompress, ByteBuf byteBuf) {
        if (rpcRequest == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new RuntimeException("连接不可用");
        }
        RpcMsg build = RpcMsg.fromFileRequest(rpcRequest);
        build.setByteBuffer(byteBuf);
        build.setNeedCompress(needCompress);
        channel.writeAndFlush(build);
    }

    public static void sendHeart(Channel channel) {
        if (channel == null || !channel.isActive()) {
            throw new RuntimeException("连接不可用");
        }
        channel.writeAndFlush(RpcMsg.fromHeart());
    }

    public static RpcFuture sendSynMsg(Channel channel, RpcRequest rpcRequest) {
        return sendSynMsg(channel, rpcRequest, NumberConstant.OVER_TIME);
    }

    public static RpcFuture sendSynMsg(Channel channel, RpcRequest rpcRequest, long timeOutMillis) {
        rpcRequest.setNeedResponse(true);
        RpcFuture rpcFuture = RpcInteractionContainer.addRequest(rpcRequest, timeOutMillis);
        sendMsg(channel, rpcRequest);
        return rpcFuture;
    }

    /**
     * 不需要针对某个请求做反应
     * 持续性接受消息,双向协议
     * 应用在: 建立会话基础上
     * a发送一条(或多条消息),b也可以发送一条或多条消息; 他们之间以会话关联
     *
     * @param channel           通道
     * @param rpcSessionRequest rpc请求
     */
    public static void sendSessionRequest(Channel channel, RpcSessionRequest rpcSessionRequest) {
        rpcSessionRequest.setSessionProcess(RpcSessionProcess.ING);
        RpcInteractionContainer.sendSessionRequest(rpcSessionRequest);
        sendMsg(channel, rpcSessionRequest);
    }

    /**
     * 不需要针对某个请求做反应
     * 持续性接受消息,双向协议
     * 应用在: 建立会话基础上
     * a发送一条(或多条消息),b也可以发送一条或多条消息; 他们之间以会话关联
     *
     * @param channel    通道
     * @param rpcSession rpc请求
     */
    public static RpcSessionFuture sendSessionStartRequest(Channel channel, RpcSession rpcSession, JSONObject context) {
        if (rpcSession == null) {
            throw new RuntimeException("rpcSession标识不能为空");
        }
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
        if (context != null) {
            rpcRequest.setBody(context.toJSONString());
        }
        if (RpcInteractionContainer.contains(rpcSession.getSessionId())) {
            throw new RuntimeException("会话已存在,请直接发送会话消息");
        }
        rpcRequest.setSessionProcess(RpcSessionProcess.START);
        RpcSessionFuture rpcFuture = RpcInteractionContainer.sendSessionRequest(rpcRequest);
        sendMsg(channel, rpcRequest);
        return rpcFuture;
    }

    /**
     * 不需要针对某个请求做反应
     * 持续性接受消息,双向协议
     * 应用在: 建立会话基础上
     * a发送一条(或多条消息),b也可以发送一条或多条消息; 他们之间以会话关联
     *
     * @param channel    通道
     * @param rpcSession rpc请求
     */
    public static void sendSessionFinishRequest(Channel channel, RpcSession rpcSession) {
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
        if (!RpcInteractionContainer.contains(rpcSession.getSessionId())) {
            throw new RuntimeException("会话不存在,无需结束会话");
        }
        rpcRequest.setSessionProcess(RpcSessionProcess.FiNISH);
        rpcRequest.setNeedResponse(false);
        RpcInteractionContainer.stopSessionGracefully(rpcSession.getSessionId());
        sendMsg(channel, rpcRequest);
    }

    /**
     * @param channel 发送文件的通道
     * @param file    文件
     */
    @SneakyThrows
    public static void writeFile(Channel channel, File file, JSONObject context, RpcFileTransHandler rpcFileTransHandler) {
        writeFile(channel, file, context, rpcFileTransHandler, null);
    }

    /**
     * @param channel 发送文件的通道
     * @param file    文件
     */
    @SneakyThrows
    public static void writeFile(Channel channel, File file, JSONObject context) {
        writeFile(channel, file, context, null);
    }

    @SneakyThrows
    private static void writeBodyFile(Channel channel, File file, long serial, ByteBuf buffer, long chunkSize, RpcSession rpcSession, boolean needCompress) {
        String fileName = file.getName();
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        rpcFileRequest.setLength(file.length());
        rpcFileRequest.setFileName(fileName);
        rpcFileRequest.setSessionProcess(RpcSessionProcess.ING);
        rpcFileRequest.setBuffer(chunkSize);
        rpcFileRequest.setSerial(serial);
        RpcMsgTransUtil.sendFileMsg(channel, rpcFileRequest, needCompress, buffer);
    }

    @SneakyThrows
    private static RpcSessionFuture writeStartFile(Channel channel, File file, JSONObject context, RpcFileTransConfig fileTransConfig, RpcSession rpcSession) {
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        rpcFileRequest.setLength(file.length());
        rpcFileRequest.setBuffer(fileTransConfig.getChunkSize());
        rpcFileRequest.setCacheBlock(fileTransConfig.getCacheBlock());
        if (context != null) {
            rpcFileRequest.setBody(context.toString());
        }
        rpcFileRequest.setFileName(file.getName());
        rpcFileRequest.setSessionProcess(RpcSessionProcess.START);
        // 设置需要返回结果
        rpcFileRequest.setNeedResponse(true);
        RpcSessionFuture rpcFuture = RpcInteractionContainer.sendSessionRequest(rpcFileRequest);
        // 发送消息体
        sendMsg(channel, rpcFileRequest);
        return rpcFuture;
    }

    @SneakyThrows
    public static void writeStopFile(Channel channel, String sessionId) {
        RpcSession rpcSession = new RpcSession(sessionId, NumberConstant.OVER_TIME);
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        rpcFileRequest.setSessionProcess(RpcSessionProcess.FiNISH);
        // 设置需要返回结果
        rpcFileRequest.setNeedResponse(false);
        RpcSessionFuture rpcSessionFuture = RpcInteractionContainer.stopSessionGracefully(rpcSession.getSessionId());
        if (rpcSessionFuture == null) {
            log.info("文件传输:已结束");
        } else {
            // 结束本地传输
            rpcSessionFuture.setSessionFinish(true);
            // 发送消息体
            sendMsg(channel, rpcFileRequest);
        }
    }

    /**
     * @param channel             要发送到的channel
     * @param file                发送的文件
     * @param rpcFileTransHandler 监听文件传输进度
     * @param rpcFileTransConfig  文件传输的限制
     * @return String             文件传输标识
     */
    public static String writeFile(Channel channel, File file, final JSONObject context, final RpcFileTransHandler rpcFileTransHandler, RpcFileTransConfig rpcFileTransConfig) {
        if (file == null || !file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        if (file.isDirectory()) {
            throw new RuntimeException("传输的文件是个目录,请检查");
        }
        final RpcFileTransConfig finalConfig = rpcFileTransConfig == null ? new RpcFileTransConfig() : rpcFileTransConfig;
        String hash = DigestUtils.md5Hex(UUID.randomUUID().toString());
        final RpcSession rpcSession = new RpcSession(hash, finalConfig.getChunkHandleTimeOut());
        RpcSessionFuture rpcFuture = writeStartFile(channel, file, context, finalConfig, rpcSession);
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        rpcFileTransProcess.setFileSize(file.length());
        rpcFileTransProcess.setRemoteHandleSize(0L);
        rpcFileTransProcess.setSendSize(0L);
        rpcFileTransProcess.setCurrentSpeed(0L);
        RpcResponse startResponse = rpcFuture.get();
        if (!startResponse.isSuccess()) {
            rpcFuture.setSessionFinish(true);
            log.warning(startResponse.getMsg());
            VirtualThreadPool.getEXECUTOR().execute(() -> rpcFileTransHandler.onRemoteFailure(file, context, startResponse.getMsg()));
            return rpcSession.getSessionId();
        }
        long writeIndex = Long.parseLong(startResponse.getBody());
        log.info("文件传输-传输开始索引:" + writeIndex + " 本地文件大小:" + file.length());
        if (writeIndex == file.length()) {
            log.info("发送方校验: 文件开始索引= 本地文件的大小,无需传输");
            if (rpcFileTransHandler != null) {
                VirtualThreadPool.getEXECUTOR().execute(() -> rpcFileTransHandler.onSuccess(file, context));
            }
            return rpcSession.getSessionId();
        }
        rpcFileTransProcess.setWriteIndex(writeIndex);
        boolean isOverWriteOnProcess = ReflectUtil.isOverridingInterfaceDefaultMethodByImplObj(rpcFileTransHandler, "onProcess");
        // 添加进度事件处理
        rpcFuture.addListener(response -> {
            if (response.isSuccess()) {
                String body = response.getBody();
                long handleSize = Long.parseLong(body);
                rpcFileTransProcess.setRemoteHandleSize(handleSize);
                if (isOverWriteOnProcess) {
                    VirtualThreadPool.getEXECUTOR().execute(() -> rpcFileTransHandler.onProcess(file, context, rpcFileTransProcess.copy()));
                }
                if (rpcFileTransHandler != null && handleSize == rpcFileTransProcess.getFileSize()) {
                    VirtualThreadPool.getEXECUTOR().execute(() -> rpcFileTransHandler.onSuccess(file, context));
                }
            } else {
                log.warning("发送端收到来自接收方的异常消息:" + response.getMsg());
                rpcFuture.setSessionFinish(true); // 标记结束
                if (rpcFileTransHandler != null) {
                    VirtualThreadPool.getEXECUTOR().execute(() -> rpcFileTransHandler.onRemoteFailure(file, context, response.getMsg()));
                }
            }
        });
        log.info("文件传输开始:" + file.getAbsolutePath());
        VirtualThreadPool.getEXECUTOR().execute(() -> {
            // 判断文件是否尝试压缩,并且适合压缩
            boolean isCompressSuitable = finalConfig.isTryCompress() && FileUtil.tryCompress(file, (int) finalConfig.getChunkSize(), finalConfig.getCompressRatePercent());
            RpcSpeedLimiter limiter = new RpcSpeedLimiter(finalConfig.getSpeedLimit());
            // 池化内存申请
            int poolSize = finalConfig.getCacheBlock();
            ByteBufPoolManager.init(rpcSession.getSessionId(), poolSize, (int) finalConfig.getChunkSize());
            // 发送头文件
            try (FileInputStream fis = new FileInputStream(file); FileChannel fileChannel = fis.getChannel()) {
                long fileSize = fileChannel.size();
                int serial = 0;
                long position = writeIndex;
                fileChannel.position(position);
                while (position < fileSize) {
                    // 校验是不是异常结束
                    if (rpcFuture.isSessionFinish()) {
                        break;
                    }
                    // **限速控制**
                    limiter.waitSpeed(100, 50);
                    // **检查连接可用性**
                    boolean isOk = RpcSpeedLimiter.waitUntil(channel::isActive, 100, 50);
                    if (!isOk) {
                        throw new RuntimeException("链接不可用");
                    }
                    boolean isWritable = RpcSpeedLimiter.waitUntil(channel::isWritable, 100, 50);
                    if (!isWritable) {
                        continue;
                    }
                    // **检测处理块数**差距
                    RpcSpeedLimiter.waitUntil(() -> (rpcFileTransProcess.getSendSize() - rpcFileTransProcess.getRemoteHandleSize()) / finalConfig.getChunkSize() < finalConfig.getCacheBlock(), 100, 50);
                    // **计算当前块大小**
                    int thisChunkSize = (int) Math.min(finalConfig.getChunkSize(), fileSize - position);
                    // **读取文件数据到 ByteBuffer**
                    ByteBuf bufferRead = ByteBufPoolManager.borrow(rpcSession.getSessionId(), rpcSession.getTimeOutMillis());
                    ByteBuffer bufferIn = bufferRead.nioBuffer(0, thisChunkSize); // 转换为nio
                    int bytesRead = fileChannel.read(bufferIn);
                    if (bytesRead < 0) {
                        break;
                    }
                    bufferRead.writerIndex(bytesRead); // 读入的数据量
                    writeBodyFile(channel, file, serial, bufferRead, finalConfig.getChunkSize(), rpcSession, isCompressSuitable);
                    serial++;
                    position += bytesRead;
                    // 已发送的数据
                    rpcFileTransProcess.setSendSize(position - writeIndex);
                    rpcFileTransProcess.setCurrentSpeed(limiter.currentSpeed().longValue());
                    // **限速控制**
                    limiter.flush(thisChunkSize);
                }
                log.info("传输完成或终止:" + file.getAbsolutePath());
            } catch (Exception e) {
                VirtualThreadPool.getEXECUTOR().execute(() -> rpcFileTransHandler.onLocalFailure(file, context, e.getMessage()));
            } finally {
                try {
                    Thread.sleep(NumberConstant.OVER_TIME);
                    ByteBufPoolManager.destory(rpcSession.getSessionId());
                    rpcFuture.release();
                } catch (InterruptedException e) {

                }
            }
        });
        return rpcSession.getSessionId();
    }

}

