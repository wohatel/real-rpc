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
import com.github.wohatel.util.RunnerUtil;
import com.github.wohatel.util.VirtualThreadPool;
import com.google.common.util.concurrent.RateLimiter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
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
public class RpcMsgTransUtil {


    public static void write(Channel channel, RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            return;
        }
        RpcSessionTransManger.flush(rpcResponse.getResponseId());
        channel.writeAndFlush(RpcMsg.fromResponse(rpcResponse));
    }

    public static void sendMsg(Channel channel, RpcRequest rpcRequest) {
        if (rpcRequest == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "连接不可用");
        }
        channel.writeAndFlush(RpcMsg.fromRequest(rpcRequest));
    }

    /**
     * 发送udp消息
     *
     */
    public static <T> void sendUdpMsg(Channel channel, T msg, InetSocketAddress to) {
        if (msg == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new IllegalStateException("Channel 不可用，发送失败");
        }
        ByteBuf buf;
        if (msg instanceof byte[] bytes) {
            // 原样发送 byte[]
            buf = Unpooled.wrappedBuffer(bytes);
        } else if (msg instanceof String s) {
            // 原样发送字符串，不加双引号
            buf = Unpooled.copiedBuffer(s, CharsetUtil.UTF_8);
        } else {
            // 对象 / 泛型 → JSON 序列化
            buf = Unpooled.wrappedBuffer(JSON.toJSONBytes(msg));
        }
        DatagramPacket packet = new DatagramPacket(buf, to);
        channel.writeAndFlush(packet);
    }

    private static void sendFileMsg(Channel channel, RpcFileRequest rpcRequest, ByteBuf byteBuf) {
        if (rpcRequest == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "连接不可用");
        }
        RpcFutureTransManager.verifySessionRequest(rpcRequest);
        RpcMsg build = RpcMsg.fromFileRequest(rpcRequest);
        build.setByteBuffer(byteBuf);
        channel.writeAndFlush(build);
    }

    public static RpcFuture sendSynMsg(Channel channel, RpcRequest rpcRequest) {
        return sendSynMsg(channel, rpcRequest, NumberConstant.OVER_TIME);
    }

    public static RpcFuture sendSynMsg(Channel channel, RpcRequest rpcRequest, long timeOutMillis) {
        rpcRequest.setNeedResponse(true);
        RpcFuture rpcFuture = RpcFutureTransManager.addRequest(rpcRequest, timeOutMillis);
        sendMsg(channel, rpcRequest);
        return rpcFuture;
    }

    @SneakyThrows
    private static void writeBodyFile(Channel channel, long serial, ByteBuf buffer, long chunkSize, RpcSession rpcSession, boolean needCompress) {
        RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
        rpcFileRequest.setSessionProcess(RpcSessionProcess.ING);
        rpcFileRequest.setBuffer(chunkSize);
        rpcFileRequest.setSerial(serial);
        rpcFileRequest.setNeedCompress(needCompress);
        RpcMsgTransUtil.sendFileMsg(channel, rpcFileRequest, buffer);
    }

    @SneakyThrows
    private static RpcSessionFuture writeStartFile(Channel channel, File file, RpcFileTransConfig fileTransConfig, RpcSession rpcSession, RpcSessionContext context) {
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
        // 发送消息体
        sendMsg(channel, rpcFileRequest);
        return rpcFuture;
    }

    @SneakyThrows
    public static void writeStopFile(Channel channel, RpcSession rpcSession) {
        RpcSessionFuture rpcSessionFuture = RpcFutureTransManager.stopSessionGracefully(rpcSession.getSessionId());
        if (rpcSessionFuture != null) {
            RpcFileRequest rpcFileRequest = new RpcFileRequest(rpcSession);
            rpcFileRequest.setSessionProcess(RpcSessionProcess.FiNISH);
            // 设置需要返回结果
            rpcFileRequest.setNeedResponse(false);
            // 发送消息体
            sendMsg(channel, rpcFileRequest);
        }
    }

    /**
     * 文件传输
     *
     * @param channel 通道
     * @param file    文件
     * @param input   输入参数
     */
    public static void writeFile(Channel channel, File file, RpcFileSenderInput input) {
        final RpcFileSenderInput fileSenderInput = input == null ? RpcFileSenderInput.builder().build() : input;
        writeFile(channel, file, fileSenderInput.getRpcSession(), fileSenderInput.getContext(), fileSenderInput.getRpcFileTransConfig(), new RpcFileSenderListenerProxy(fileSenderInput.getRpcFileSenderListener()));
    }

    /**
     * @param channel            要发送到的channel
     * @param file               发送的文件
     * @param rpcFileTransConfig 文件传输的限制
     */
    private static void writeFile(Channel channel, File file, final RpcSession rpcSession, RpcSessionContext context, RpcFileTransConfig rpcFileTransConfig, RpcFileSenderListenerProxy listener) {
        if (file == null || !file.exists()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "文件不存在");
        }
        if (file.isDirectory()) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "传输的文件是个目录,请检查");
        }
        if (rpcSession == null) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession 不能为null,请检查");
        }
        boolean contains = RpcFutureTransManager.contains(rpcSession.getSessionId());
        if (contains) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession 会话已存在,请检查rpcSession是否重复使用");
        }
        boolean running = RpcSessionTransManger.isRunning(rpcSession.getSessionId());
        if (running) {
            throw new RpcException(RpcErrorEnum.SEND_MSG, "rpcSession 会话已存在,请更换新的会话");
        }
        final RpcFileTransConfig finalConfig = rpcFileTransConfig == null ? RpcFileTransConfig.builder().build() : rpcFileTransConfig;
        // 封装进度
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        rpcFileTransProcess.setFileLength(file.length());
        rpcFileTransProcess.setSendSize(0L);
        rpcFileTransProcess.setRemoteHandleSize(0L);
        RpcSessionFuture rpcFuture = writeStartFile(channel, file, finalConfig, rpcSession, context);
        RpcResponse startResponse = rpcFuture.get();
        if (!startResponse.isSuccess()) {
            throw new RpcException(RpcErrorEnum.RUNTIME, "远程执行传输文件失败:" + startResponse.getMsg());
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
            runSendFileBody(channel, file, rpcFileSenderWrapper, rpcFuture, rpcFileTransProcess, finalConfig, listener);
        } else {
            // 失败时执行
            if (StringUtils.isNotBlank(startResponse.getMsg())) {
                // 失败
                listener.onFailure(rpcFileSenderWrapper, startResponse.getMsg());
            } else {
                // 成功的时候
                listener.onSuccess(rpcFileSenderWrapper);
            }
        }
    }

    private static void runSendFileBody(Channel channel, File file, RpcFileSenderWrapper rpcFileSenderWrapper, RpcSessionFuture rpcFuture, RpcFileTransProcess rpcFileTransProcess, final RpcFileTransConfig finalConfig, RpcFileSenderListenerProxy listener) {
        // 添加进度事件处理
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
                        releaseFileTrans(rpcFileSenderWrapper.getRpcSession(), rpcFuture);
                    }
                } else {
                    log.error("发送端收到来自接收方的异常消息:" + response.getMsg() + JsonUtil.toJson(response));
                    rpcFuture.setRpcSessionProcess(RpcSessionProcess.FiNISH); // 标记结束
                    listener.onFailure(rpcFileSenderWrapper, response.getMsg());
                    releaseFileTrans(rpcFileSenderWrapper.getRpcSession(), rpcFuture);
                }
            }

            @Override
            public void onTimeout() {
                releaseFileTrans(rpcFileSenderWrapper.getRpcSession(), rpcFuture);
            }

            @Override
            public void onSessionInterrupt() {
                releaseFileTrans(rpcFileSenderWrapper.getRpcSession(), rpcFuture);
            }
        };
        VirtualThreadPool.execute(() -> rpcFuture.addListener(rpcResponseMsgListener));

        log.info("文件传输开始:" + file.getAbsolutePath());
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
                if (rpcFuture.isSessionFinish()) {
                    break;
                }
                if (!channel.isActive()) {
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "链接不可用");
                }
                boolean isWritable = RunnerUtil.waitUntil(channel::isWritable, 100, rpcSession.getTimeOutMillis() / 100);
                if (!isWritable) {
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "文件发送超时");
                }
                // **检测处理块数**差距
                RunnerUtil.waitUntil(() -> (rpcFileTransProcess.getSendSize() - rpcFileTransProcess.getRemoteHandleSize()) / finalConfig.getChunkSize() < finalConfig.getCacheBlock(), 100, 50);
                // **计算当前块大小**
                int thisChunkSize = (int) Math.min(finalConfig.getChunkSize(), fileSize - position);
                // **限速控制**
                boolean isEnough = rateLimiter.tryAcquire(thisChunkSize, rpcSession.getTimeOutMillis(), TimeUnit.MILLISECONDS);
                if (!isEnough) {
                    throw new RpcException(RpcErrorEnum.SEND_MSG, "文件发送超时: 限速过低");
                }
                // **读取文件数据到 ByteBuffer**
                ByteBuf bufferRead = ByteBufPoolManager.borrow(rpcSession.getSessionId(), rpcSession.getTimeOutMillis());
                ByteBuffer bufferIn = bufferRead.nioBuffer(0, thisChunkSize); // 转换为nio
                int bytesRead = fileChannel.read(bufferIn);
                if (bytesRead < 0) {
                    break;
                }
                bufferRead.writerIndex(bytesRead); // 读入的数据量
                writeBodyFile(channel, serial, bufferRead, finalConfig.getChunkSize(), rpcSession, isCompressSuitable);
                serial++;
                position += bytesRead;
                // 已发送的数据
                rpcFileTransProcess.setSendSize(position - writeIndex);
            }
        } catch (Exception e) {
            rpcFuture.setRpcSessionProcess(RpcSessionProcess.FiNISH);
            log.error("文件块-发送-打印异常信息:", e);
            listener.onFailure(rpcFileSenderWrapper, e.getMessage());
        }
    }

    private static void releaseFileTrans(RpcSession rpcSession, RpcSessionFuture rpcSessionFuture) {
        VirtualThreadPool.execute(() -> {
            ByteBufPoolManager.destory(rpcSession.getSessionId());
            rpcSessionFuture.release();
        });
    }

    public static String sendInquiryRemoteNodeIdRequest(Channel channel) {
        // 同源的看谁发起的,不通源的看
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setRequestType(RpcBaseAction.BASE_INQUIRY_SESSION.name());
        RpcFuture rpcFuture = sendSynMsg(channel, rpcRequest);
        RpcResponse rpcResponse = rpcFuture.get();
        if (rpcResponse.isSuccess()) {
            return rpcResponse.getBody();
        }
        return null;
    }
}

