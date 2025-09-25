package com.github.wohatel.interaction.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.wohatel.constant.RpcBaseAction;
import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcFuture;
import com.github.wohatel.interaction.base.RpcMsg;
import com.github.wohatel.interaction.base.RpcRequest;
import com.github.wohatel.interaction.base.RpcResponse;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.base.RpcSessionFuture;
import com.github.wohatel.interaction.base.RpcSessionProcess;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileInfo;
import com.github.wohatel.interaction.file.RpcFileRequest;
import com.github.wohatel.interaction.file.RpcFileSenderInput;
import com.github.wohatel.interaction.file.RpcFileSenderListenerProxy;
import com.github.wohatel.interaction.file.RpcFileSenderWrapper;
import com.github.wohatel.interaction.file.RpcFileTransConfig;
import com.github.wohatel.interaction.file.RpcFileTransModel;
import com.github.wohatel.interaction.file.RpcFileTransProcess;
import com.github.wohatel.util.FileUtil;
import com.github.wohatel.util.JsonUtil;
import com.github.wohatel.util.Pooled;
import com.github.wohatel.util.RunnerUtil;
import com.github.wohatel.util.VirtualThreadPool;
import com.google.common.util.concurrent.RateLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcMsgTransManager {


    public static void sendResponse(Channel channel, RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            return;
        }
        channel.writeAndFlush(RpcMsg.fromResponse(rpcResponse));
    }

    public static void sendRequest(Channel channel, RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "connection is not available");
        }
        channel.writeAndFlush(RpcMsg.fromRequest(rpcRequest));
    }

    /**
     * 发送udp消息
     */
    public static <T> void sendUdpMsg(Channel channel, T msg, InetSocketAddress to) {
        if (msg == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Channel unavailable, send failed");
        }
        ByteBuf buf;
        if (msg instanceof byte[] bytes) {
            // 原样发送 byte[]
            buf = Pooled.wrappedBuffer(bytes);
        } else if (msg instanceof String s) {
            // 原样发送字符串，不加双引号
            buf = Pooled.copiedBuffer(s, CharsetUtil.UTF_8);
            buf = Pooled.copiedBuffer(s, CharsetUtil.UTF_8);
        } else {
            // 对象 / 泛型 → JSON 序列化
            buf = Pooled.wrappedBuffer(JSON.toJSONBytes(msg));
        }
        DatagramPacket packet = new DatagramPacket(buf, to);
        channel.writeAndFlush(packet);
    }

    /**
     * 发送文件块
     */
    private static ChannelFuture sendFileOfSendTrunk(Channel channel, RpcFileRequest rpcRequest, ByteBuf byteBuf) {
        if (channel == null || !channel.isActive()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "connection is not available");
        }
        RpcFutureTransManager.verifySessionRequest(rpcRequest);
        RpcMsg build = RpcMsg.fromFileRequest(rpcRequest);
        build.setByteBuffer(byteBuf);
        return channel.writeAndFlush(build);
    }

    /**
     * 发送消息,并附带响应结果
     *
     * @param channel    通道
     * @param rpcRequest 请求
     * @return rpcfuture
     */
    public static RpcFuture sendSynRequest(Channel channel, RpcRequest rpcRequest) {
        return sendSynRequest(channel, rpcRequest, NumberConstant.OVER_TIME);
    }

    /**
     * 发送消息,并附带响应结果
     *
     * @param channel       通道
     * @param rpcRequest    请求
     * @param timeOutMillis 设定超时时间
     */
    public static RpcFuture sendSynRequest(Channel channel, RpcRequest rpcRequest, long timeOutMillis) {
        rpcRequest.setNeedResponse(true);
        RpcFuture rpcFuture = RpcFutureTransManager.addRequest(rpcRequest, timeOutMillis);
        sendRequest(channel, rpcRequest);
        return rpcFuture;
    }

    /**
     * 发送文件body体
     */
    @SneakyThrows
    private static void sendFileOfSendBody(Channel channel, long serial, ByteBuf buffer, long chunkSize, RpcSession rpcSession, boolean needCompress, boolean finished) {
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        rpcFileRequest.setSessionProcess(RpcSessionProcess.ING);
        rpcFileRequest.setBuffer(chunkSize);
        rpcFileRequest.setSerial(serial);
        rpcFileRequest.setEnableCompress(needCompress);
        rpcFileRequest.setFinished(finished);
        RpcMsgTransManager.sendFileOfSendTrunk(channel, rpcFileRequest, buffer);
    }

    /**
     * 发送文件开启session
     */
    @SneakyThrows
    private static RpcSessionFuture sendFileOfStartSession(Channel channel, File file, RpcFileTransConfig fileTransConfig, RpcSession rpcSession, RpcSessionContext context) {
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        RpcFileInfo rpcFileInfo = new RpcFileInfo();
        rpcFileInfo.setFileName(file.getName());
        rpcFileInfo.setLength(file.length());
        if (fileTransConfig.isSendFileMd5()) {
            rpcFileInfo.setFileMd5(FileUtil.md5(file));
        }
        rpcFileRequest.setFileInfo(rpcFileInfo);
        rpcFileRequest.setBuffer(fileTransConfig.getChunkSize());
        rpcFileRequest.setCacheBlock(fileTransConfig.getCacheBlock());
        rpcFileRequest.setSessionProcess(RpcSessionProcess.START);
        if (context != null) {
            rpcFileRequest.setBody(JSONObject.toJSONString(context));
        }
        // 设置需要返回结果
        rpcFileRequest.setNeedResponse(true);
        RpcSessionFuture rpcFuture = RpcFutureTransManager.verifySessionRequest(rpcFileRequest);
        rpcFuture.setChannelId(channel.id().asShortText());
        // 发送消息体
        sendRequest(channel, rpcFileRequest);
        return rpcFuture;
    }

    @SneakyThrows
    public static void interruptSendFile(Channel channel, RpcSession rpcSession) {
        RpcSessionFuture rpcSessionFuture = RpcFutureTransManager.stopSessionGracefully(rpcSession.getSessionId(), channel.id().asShortText());
        if (rpcSessionFuture != null) {
            RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
            rpcFileRequest.setSessionProcess(RpcSessionProcess.FiNISH);
            // 设置需要返回结果
            rpcFileRequest.setNeedResponse(false);
            // 发送消息体
            sendRequest(channel, rpcFileRequest);
        }
        // 清理内存池
        ByteBufPoolManager.destory(rpcSession.getSessionId());
    }

    @SneakyThrows
    public static void interruptReceiveFile(RpcSession rpcSession) {
        RpcSessionTransManger.releaseFile(rpcSession.getSessionId());
    }

    /**
     * 文件传输
     *
     * @param channel 通道
     * @param file    文件
     * @param input   输入参数
     */
    public static void sendFile(Channel channel, File file, RpcFileSenderInput input) {
        final RpcFileSenderInput fileSenderInput = input == null ? RpcFileSenderInput.builder().build() : input;
        sendFile(channel, file, fileSenderInput.getRpcSession(), fileSenderInput.getContext(), fileSenderInput.getRpcFileTransConfig(), new RpcFileSenderListenerProxy(fileSenderInput.getRpcFileSenderListener()));
    }

    /**
     * @param channel            要发送到的channel
     * @param file               发送的文件
     * @param rpcFileTransConfig 文件传输的限制
     */
    private static void sendFile(Channel channel, File file, final RpcSession rpcSession, RpcSessionContext context, RpcFileTransConfig rpcFileTransConfig, RpcFileSenderListenerProxy listener) {
        if (file == null || !file.exists()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the file does not exist");
        }
        if (file.isDirectory()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "the transferred files are a directory, please check them");
        }
        if (rpcSession == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession It can't be null, please check");
        }
        boolean contains = RpcFutureTransManager.contains(rpcSession.getSessionId());
        if (contains) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession The session already exists, check if the rpcSession is reused");
        }
        boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
        if (running) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession The session already exists, please replace it with a new one");
        }
        final RpcFileTransConfig finalConfig = rpcFileTransConfig == null ? RpcFileTransConfig.builder().build() : rpcFileTransConfig;
        // 封装进度
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        rpcFileTransProcess.setFileLength(file.length());
        rpcFileTransProcess.setSendSize(0L);
        rpcFileTransProcess.setRemoteHandleSize(0L);
        RpcSessionFuture rpcFuture = sendFileOfStartSession(channel, file, finalConfig, rpcSession, context);
        RpcResponse startResponse = rpcFuture.get();
        if (!startResponse.isSuccess()) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "remote execution of file transfer failed:" + startResponse.getMsg());
        }
        rpcFuture.setRpcSessionProcess(RpcSessionProcess.ING);
        String responseBody = startResponse.getBody();
        JSONArray array = JSONArray.parseArray(responseBody);
        Boolean needTrans = array.getBoolean(0);
        RpcFileTransModel transModel = RpcFileTransModel.nameOf(array.getString(1));
        Long writeIndex = array.getLong(2);
        String filePath = array.getString(3);
        rpcFileTransProcess.setStartIndex(writeIndex);
        // 汇总校验结果
        RpcFileSenderWrapper rpcFileSenderWrapper = new RpcFileSenderWrapper(rpcSession, file, transModel);
        if (needTrans) {
            // 监听文件发送状态
            listenSendFileBody(rpcFileSenderWrapper, rpcFuture, rpcFileTransProcess, listener);
            // 发送文件数据
            sendFileBody(channel, file, rpcFileSenderWrapper, rpcFuture, rpcFileTransProcess, finalConfig, listener);
            return;
        }
        // 失败时执行
        if (StringUtils.isNotBlank(startResponse.getMsg())) {
            // 失败
            listener.onFailure(rpcFileSenderWrapper, startResponse.getMsg());
        } else {
            // 成功的时候
            listener.onSuccess(rpcFileSenderWrapper);
        }

    }

    /**
     * 监听文件发送
     *
     * @param rpcFileSenderWrapper 传输描述
     * @param rpcFuture            future
     * @param rpcFileTransProcess  传输进度
     * @param listener             listener执行器
     */
    private static void listenSendFileBody(RpcFileSenderWrapper rpcFileSenderWrapper, RpcSessionFuture rpcFuture, RpcFileTransProcess rpcFileTransProcess, RpcFileSenderListenerProxy listener) {
        RpcResponseMsgListener rpcResponseMsgListener = new RpcResponseMsgListener() {
            @Override
            public void onResponse(RpcResponse response) {
                if (response.isSuccess()) {
                    String body = response.getBody();
                    long handleSize = Long.parseLong(body);
                    rpcFileTransProcess.setRemoteHandleSize(handleSize);
                    listener.onProcess(rpcFileSenderWrapper, rpcFileTransProcess.copy());
                    if (handleSize == rpcFileTransProcess.getFileLength() - rpcFileTransProcess.getStartIndex()) {
                        listener.onSuccess(rpcFileSenderWrapper);
                        rpcFuture.release();
                        ByteBufPoolManager.destory(rpcFileSenderWrapper.getRpcSession().getSessionId());
                    }
                } else {
                    log.error("The sender receives an exception message from the receiver:" + response.getMsg() + JsonUtil.toJson(response));
                    rpcFuture.setRpcSessionProcess(RpcSessionProcess.FiNISH); // 标记结束
                    listener.onFailure(rpcFileSenderWrapper, response.getMsg());
                    rpcFuture.release();
                    ByteBufPoolManager.destory(rpcFileSenderWrapper.getRpcSession().getSessionId());
                }
            }

            @Override
            public void onTimeout() {
                log.error("send file time out");
                rpcFuture.release();
                ByteBufPoolManager.destory(rpcFileSenderWrapper.getRpcSession().getSessionId());
            }

            @Override
            public void onSessionInterrupt() {
                log.info("send file was interrupt");
                rpcFuture.release();
                ByteBufPoolManager.destory(rpcFileSenderWrapper.getRpcSession().getSessionId());
            }
        };
        VirtualThreadPool.execute(() -> rpcFuture.addListener(rpcResponseMsgListener));
    }

    private static void sendFileBody(Channel channel, File file, RpcFileSenderWrapper rpcFileSenderWrapper, RpcSessionFuture rpcFuture, RpcFileTransProcess rpcFileTransProcess, final RpcFileTransConfig finalConfig, RpcFileSenderListenerProxy listener) {
        // 开始发送
        log.info("the file transfer begins:" + file.getAbsolutePath());
        RpcSession rpcSession = rpcFileSenderWrapper.getRpcSession();
        // 判断文件是否尝试压缩,并且适合压缩
        boolean isCompressSuitable = finalConfig.isTryCompress() && FileUtil.tryCompress(file, (int) finalConfig.getChunkSize(), finalConfig.getCompressRatePercent());
        RateLimiter rateLimiter = RateLimiter.create(finalConfig.getSpeedLimit());
        Long writeIndex = rpcFileTransProcess.getStartIndex();
        // 池化内存申请
        int poolSize = finalConfig.getCacheBlock();
        int applyMemory = Math.min(poolSize, NumberConstant.EIGHT);
        ByteBufPoolManager.init(rpcSession.getSessionId(), applyMemory, (int) finalConfig.getChunkSize());
        // 发送头文件
        try (FileInputStream fis = new FileInputStream(file); FileChannel fileChannel = fis.getChannel()) {
            long fileSize = fileChannel.size();
            int serial = 0;
            long position = writeIndex;
            fileChannel.position(position);
            while (position < fileSize) {
                // 校验是不是异常结束
                log.info("the file transfer begins:" + position + " size:" + fileSize);
                if (rpcFuture.isSessionFinish()) {
                    break;
                }
                if (!RpcFutureTransManager.contains(rpcSession.getSessionId())) {
                    break;
                }
                if (!channel.isActive()) {
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "the link is not available");
                }
                boolean isWritable = RunnerUtil.waitUntil(channel::isWritable, 100, rpcSession.getTimeOutMillis() / 100);
                if (!isWritable) {
                    log.error("the link is not available");
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "file sending timeout");
                }
                // **检测处理块数**差距
                RunnerUtil.waitUntil(() -> (rpcFileTransProcess.getSendSize() - rpcFileTransProcess.getRemoteHandleSize()) / finalConfig.getChunkSize() < finalConfig.getCacheBlock(), 100, 50);
                // **计算当前块大小**
                int thisChunkSize = (int) Math.min(finalConfig.getChunkSize(), fileSize - position);
                // **限速控制**
                boolean isEnough = rateLimiter.tryAcquire(thisChunkSize, rpcSession.getTimeOutMillis(), TimeUnit.MILLISECONDS);
                if (!isEnough) {
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "file Send Timeout: The speed limit is too low");
                }
                // **读取文件数据到 ByteBuffer**
                ByteBuf bufferRead = ByteBufPoolManager.borrow(rpcSession.getSessionId(), rpcSession.getTimeOutMillis());
                ByteBuffer bufferIn = bufferRead.nioBuffer(0, thisChunkSize); // 转换为nio
                int bytesRead = fileChannel.read(bufferIn);
                if (bytesRead < 0) {
                    break;
                }
                bufferRead.writerIndex(bytesRead); // 读入的数据量
                position += bytesRead;
                // 已发送的数据
                rpcFileTransProcess.setSendSize(position - writeIndex);
                sendFileOfSendBody(channel, serial, bufferRead, finalConfig.getChunkSize(), rpcSession, isCompressSuitable, position >= fileSize);
                // 序号++
                serial++;
            }
        } catch (Exception e) {
            rpcFuture.setRpcSessionProcess(RpcSessionProcess.FiNISH);
            log.error("file block - send - print abnormal information:", e);
            listener.onFailure(rpcFileSenderWrapper, e.getMessage());
            ByteBufPoolManager.destory(rpcFileSenderWrapper.getRpcSession().getSessionId());
        }
    }

    public static String sendInquiryRemoteNodeIdRequest(Channel channel) {
        // 同源的看谁发起的,不通源的看
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setContentType(RpcBaseAction.BASE_INQUIRY_SESSION.name());
        RpcFuture rpcFuture = sendSynRequest(channel, rpcRequest);
        RpcResponse rpcResponse = rpcFuture.get();
        if (rpcResponse.isSuccess()) {
            return rpcResponse.getBody();
        }
        return null;
    }
}

