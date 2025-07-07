package com.murong.rpc.interaction.common;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import com.murong.rpc.interaction.base.RpcFuture;
import com.murong.rpc.interaction.base.RpcMsg;
import com.murong.rpc.interaction.base.RpcRequest;
import com.murong.rpc.interaction.base.RpcResponse;
import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.base.RpcSessionFuture;
import com.murong.rpc.interaction.base.RpcSessionProcess;
import com.murong.rpc.interaction.base.RpcSessionRequest;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.file.RpcFileInfo;
import com.murong.rpc.interaction.file.RpcFileRemoteWrapper;
import com.murong.rpc.interaction.file.RpcFileRequest;
import com.murong.rpc.interaction.file.RpcFileTransConfig;
import com.murong.rpc.interaction.file.RpcFileTransModel;
import com.murong.rpc.interaction.file.RpcFileTransProcess;
import com.murong.rpc.interaction.handler.RpcFileTransHandler;
import com.murong.rpc.util.FileUtil;
import com.murong.rpc.util.JsonUtil;
import com.murong.rpc.util.ReflectUtil;
import com.murong.rpc.util.RunnerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcMsgTransUtil {

    public static void write(Channel channel, RpcResponse rpcResponse) {
        if (rpcResponse == null) {
            return;
        }
        TransSessionManger0.flush(rpcResponse.getRequestId());
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

    public static void sendFileMsg(Channel channel, RpcFileRequest rpcRequest, ByteBuf byteBuf) {
        if (rpcRequest == null) {
            return;
        }
        if (channel == null || !channel.isActive()) {
            throw new RuntimeException("连接不可用");
        }
        RpcInteractionContainer.verifySessionRequest(rpcRequest);
        RpcMsg build = RpcMsg.fromFileRequest(rpcRequest);
        build.setByteBuffer(byteBuf);
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
        RpcSession rpcSession = rpcSessionRequest.getRpcSession();
        RpcSessionFuture sessionFuture = RpcInteractionContainer.getSessionFuture(rpcSession.getSessionId());
        if (sessionFuture == null) {
            throw new RuntimeException("会话不存在,请尝试开启新的会话");
        }
        if (sessionFuture.isSessionFinish()) {
            throw new RuntimeException("会话已结束,请尝试开启新的会话");
        }
        rpcSessionRequest.setSessionProcess(RpcSessionProcess.ING);
        RpcInteractionContainer.verifySessionRequest(rpcSessionRequest);
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
    public static RpcSessionFuture sendSessionStartRequest(Channel channel, RpcSession rpcSession) {
        return sendSessionStartRequest(channel, rpcSession, null);
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
    public static RpcSessionFuture sendSessionStartRequest(Channel channel, RpcSession rpcSession, RpcSessionContext context) {
        if (rpcSession == null) {
            throw new RuntimeException("rpcSession标识不能为空");
        }
        if (RpcInteractionContainer.contains(rpcSession.getSessionId())) {
            throw new RuntimeException("会话已存在,请直接发送会话消息");
        }
        if (TransSessionManger0.isRunning(rpcSession.getSessionId())) {
            throw new RuntimeException("会话已存在,请创建新会话");
        }
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
        rpcRequest.setSessionProcess(RpcSessionProcess.START);
        if (context != null) {
            rpcRequest.setBody(JSONObject.toJSONString(context));
        }
        RpcSessionFuture rpcFuture = RpcInteractionContainer.verifySessionRequest(rpcRequest);
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
        if (!RpcInteractionContainer.contains(rpcSession.getSessionId())) {
            return;
        }
        RpcSessionRequest rpcRequest = new RpcSessionRequest(rpcSession);
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
    public static void writeFile(Channel channel, File file, RpcSession rpcSession, RpcSessionContext context, RpcFileTransHandler rpcFileTransHandler) {
        writeFile(channel, file, rpcSession, context, rpcFileTransHandler, null);
    }

    /**
     * @param channel 发送文件的通道
     * @param file    文件
     */
    @SneakyThrows
    public static void writeFile(Channel channel, File file, RpcSession rpcSession, RpcSessionContext context) {
        writeFile(channel, file, rpcSession, context, null);
    }

    /**
     * @param channel 发送文件的通道
     * @param file    文件
     */
    @SneakyThrows
    public static RpcSession writeFile(Channel channel, File file, RpcSessionContext context) {
        RpcSession rpcSession = new RpcSession(NumberConstant.TEN_EIGHT_K);
        writeFile(channel, file, rpcSession, context);
        return rpcSession;
    }

    /**
     * @param channel 发送文件的通道
     * @param file    文件
     */
    @SneakyThrows
    public static void writeFile(Channel channel, File file, RpcSession rpcSession) {
        writeFile(channel, file, rpcSession, null);
    }

    /**
     * @param channel 发送文件的通道
     * @param file    文件
     */
    @SneakyThrows
    public static RpcSession writeFile(Channel channel, File file) {
        RpcSession rpcSession = new RpcSession(NumberConstant.TEN_EIGHT_K);
        writeFile(channel, file, rpcSession, null);
        return rpcSession;
    }

    @SneakyThrows
    private static void writeBodyFile(Channel channel, File file, long serial, ByteBuf buffer, long chunkSize, RpcSession rpcSession, boolean needCompress) {
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
            rpcFileInfo.setFileHash(FileUtil.fileSha256Hash(file));
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
        RpcSessionFuture rpcFuture = RpcInteractionContainer.verifySessionRequest(rpcFileRequest);
        // 发送消息体
        sendMsg(channel, rpcFileRequest);
        return rpcFuture;
    }

    @SneakyThrows
    public static void writeStopFile(Channel channel, RpcSession rpcSession) {
        RpcSessionFuture rpcSessionFuture = RpcInteractionContainer.stopSessionGracefully(rpcSession.getSessionId());
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
     * @param channel             要发送到的channel
     * @param file                发送的文件
     * @param rpcFileTransHandler 监听文件传输进度
     * @param rpcFileTransConfig  文件传输的限制
     * @return String             文件传输标识
     */
    public static void writeFile(Channel channel, File file, final RpcSession rpcSession, RpcSessionContext context, final RpcFileTransHandler rpcFileTransHandler, RpcFileTransConfig rpcFileTransConfig) {
        if (file == null || !file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        if (file.isDirectory()) {
            throw new RuntimeException("传输的文件是个目录,请检查");
        }
        if (rpcSession == null) {
            throw new RuntimeException("rpcSession 不能为null,请检查");
        }
        boolean contains = RpcInteractionContainer.contains(rpcSession.getSessionId());
        if (contains) {
            throw new RuntimeException("rpcSession 会话已存在,请检查rpcSession是否重复使用");
        }
        boolean running = TransSessionManger0.isRunning(rpcSession.getSessionId());
        if (running) {
            throw new RuntimeException("rpcSession 会话已存在,请更换新的会话");
        }
        final RpcFileTransConfig finalConfig = rpcFileTransConfig == null ? new RpcFileTransConfig() : rpcFileTransConfig;
        // 封装进度
        RpcFileTransProcess rpcFileTransProcess = new RpcFileTransProcess();
        rpcFileTransProcess.setFileSize(file.length());
        rpcFileTransProcess.setSendSize(0L);
        rpcFileTransProcess.setRemoteHandleSize(0L);

        RpcSessionFuture rpcFuture = writeStartFile(channel, file, finalConfig, rpcSession, context);
        RpcResponse startResponse = rpcFuture.get();
        String responseBody = startResponse.getBody();
        JSONArray array = JSONArray.parseArray(responseBody);
        Boolean needTrans = array.getBoolean(0);
        RpcFileTransModel transModel = RpcFileTransModel.nameOf(array.getString(1));
        Long writeIndex = array.getLong(2);
        String filePath = array.getString(3);
        RpcFileRemoteWrapper rpcFileRemoteWrapper = new RpcFileRemoteWrapper(filePath, transModel);
        if (!needTrans) {
            rpcFuture.setSessionFinish(true);
            if (StringUtils.isBlank(startResponse.getMsg())) {
                VirtualThreadPool.execute(rpcFileTransHandler != null, () -> rpcFileTransHandler.onSuccess(file, rpcFileRemoteWrapper));
            } else {
                VirtualThreadPool.execute(rpcFileTransHandler != null, () -> rpcFileTransHandler.onFailure(file, rpcFileRemoteWrapper, startResponse.getMsg()));
            }
            log.warning("接收方不接收文件");
            return;
        }
        boolean isOverWriteOnProcess = ReflectUtil.isOverridingInterfaceDefaultMethodByImplObj(rpcFileTransHandler, "onProcess");
        // 添加进度事件处理
        AtomicReference<String> errorMsg = new AtomicReference<>();
        rpcFuture.addListener(response -> {
            if (response.isSuccess()) {
                String body = response.getBody();
                long handleSize = Long.parseLong(body);
                rpcFileTransProcess.setRemoteHandleSize(handleSize);
                if (isOverWriteOnProcess) {
                    VirtualThreadPool.execute(() -> rpcFileTransHandler.onProcess(file, rpcFileRemoteWrapper, rpcFileTransProcess.copy()));
                }
                if (handleSize == rpcFileTransProcess.getFileSize()) {
                    VirtualThreadPool.execute(rpcFileTransHandler != null, () -> rpcFileTransHandler.onSuccess(file, rpcFileRemoteWrapper));
                }
            } else {
                log.warning("发送端收到来自接收方的异常消息:" + response.getMsg() + JsonUtil.toJson(response));
                rpcFuture.setSessionFinish(true); // 标记结束
                errorMsg.set(response.getMsg());
            }
        });
        log.info("文件传输开始:" + file.getAbsolutePath());
        VirtualThreadPool.execute(() -> {
            // 判断文件是否尝试压缩,并且适合压缩
            boolean isCompressSuitable = finalConfig.isTryCompress() && FileUtil.tryCompress(file, (int) finalConfig.getChunkSize(), finalConfig.getCompressRatePercent());
            RateLimiter rateLimiter = RateLimiter.create(finalConfig.getSpeedLimit());
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
                        throw new RuntimeException("链接不可用");
                    }
                    boolean isWritable = RunnerUtil.waitUntil(channel::isWritable, 100, rpcSession.getTimeOutMillis() / 100);
                    if (!isWritable) {
                        throw new RuntimeException("文件发送超时");
                    }
                    // **检测处理块数**差距
                    RunnerUtil.waitUntil(() -> (rpcFileTransProcess.getSendSize() - rpcFileTransProcess.getRemoteHandleSize()) / finalConfig.getChunkSize() < finalConfig.getCacheBlock(), 100, 50);
                    // **计算当前块大小**
                    int thisChunkSize = (int) Math.min(finalConfig.getChunkSize(), fileSize - position);
                    // **限速控制**
                    boolean isEnough = rateLimiter.tryAcquire(thisChunkSize, rpcSession.getTimeOutMillis(), TimeUnit.MILLISECONDS);
                    if (!isEnough) {
                        throw new RuntimeException("文件发送超时: 限速过低");
                    }
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
                }
                String transStatus = position < fileSize ? "中止" : "完成";
                log.info("传输" + transStatus + ":" + file.getAbsolutePath());
            } catch (Exception e) {
                log.log(Level.WARNING, "传输文件异常:", e);
                errorMsg.set(e.getMessage());
            } finally {
                try {
                    VirtualThreadPool.execute(rpcFileTransHandler != null, () -> rpcFileTransHandler.onFailure(file, rpcFileRemoteWrapper, errorMsg.get()));
                    Thread.sleep(NumberConstant.ONE_POINT_FILE_K);
                    ByteBufPoolManager.destory(rpcSession.getSessionId());
                    rpcFuture.release();
                } catch (InterruptedException e) {

                }
            }

        });
    }
}

