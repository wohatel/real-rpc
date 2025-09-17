package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.constant.NumberConstant;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.util.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * description
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcSessionTransManger {

    /**
     * 文件块的释放,需要比较久的时间,为了避免单线程造成的资源关闭堆积,才出采用线程池= true
     */
    private static final SessionManager<SessionDataWrapper> SESSION_MANAGER = new SessionManager<>(NumberConstant.TEN, RpcSessionTransManger::release);
    private static final Map<String, BlockingQueue<FileChunkItem>> FILE_ITEM_MAP = new ConcurrentHashMap<>();
    private static final Map<String, RpcSession> SESSION = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class SessionDataWrapper {
        private boolean isFile;
        private RpcSessionContext context;
        private RpcFileReceiveWrapper rpcFileReceiveWrapper;
    }

    public static void initSession(RpcSessionContext context, RpcSession rpcSession) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.CONNECT, "session已存在");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(false, context, null);
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        SESSION.put(sessionId, rpcSession);
    }

    /**
     * 获取sessionContext
     */
    public static RpcSessionContext getSessionContext(String sessionId) {
        return SESSION_MANAGER.getSession(sessionId).context;
    }

    /**
     * 刷新时间
     *
     * @param sessionId   会话id
     * @param sessionTime 会话保留时长
     */
    public static boolean flush(String sessionId, long sessionTime) {
        return SESSION_MANAGER.flushTime(sessionId, sessionTime);
    }

    /**
     * 刷新时间
     */
    public static boolean flush(String sessionId) {
        RpcSession rpcSession = SESSION.get(sessionId);
        if (rpcSession != null) {
            return flush(sessionId, rpcSession.getTimeOutMillis());
        }
        return false;
    }

    public static boolean addOrReleaseFile(String sessionId, FileChunkItem fileChunkItem) {
        try {
            SessionDataWrapper session = SESSION_MANAGER.getSession(sessionId);
            if (session.isFile) {
                FILE_ITEM_MAP.get(sessionId).add(fileChunkItem);
                SESSION_MANAGER.flushTime(sessionId);
                return true;
            } else {
                releaseFileChunk(fileChunkItem);
                return false;
            }
        } catch (Exception e) {
            log.error("文件块-接收-打印异常信息:", e);
            releaseFileChunk(fileChunkItem);
            return false;
        }
    }

    /**
     * 是否正常运行
     */
    public static boolean isRunning(String sessionId) {
        return SESSION_MANAGER.contains(sessionId);
    }

    public static void initFile(RpcSession rpcSession, int cacheBlock, RpcFileReceiveWrapper data) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.HANDLE_MSG, "文件session已存在");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(true, null, data);
        PriorityBlockingQueue<FileChunkItem> queue = new PriorityBlockingQueue<>(cacheBlock + 1, Comparator.comparingLong(FileChunkItem::getSerial));
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        FILE_ITEM_MAP.put(sessionId, queue);
        SESSION.put(sessionId, rpcSession);
    }

    @SneakyThrows
    public static FileChunkItem poll(String sessionId, long timeMills) {
        FileChunkItem poll = FILE_ITEM_MAP.get(sessionId).poll(timeMills, TimeUnit.MILLISECONDS);
        if (poll != null) {
            SESSION_MANAGER.flushTime(sessionId);
        }
        return poll;
    }

    public static RpcFileReceiveWrapper getFileData(String sessionId) {
        return SESSION_MANAGER.getSession(sessionId).rpcFileReceiveWrapper;
    }

    /**
     * 小窗口机制延迟结束
     */
    public static void closeQueue(BlockingQueue<FileChunkItem> queue) {
        if (queue == null) {
            return;
        }
        FileChunkItem poll;
        while (true) {
            try {
                // 先无等待poll
                poll = queue.poll();
                if (poll != null) {
                    ReferenceCountUtil.safeRelease(poll.getByteBuf());
                    continue;
                }
                // 再等待1秒
                poll = queue.poll(1, TimeUnit.SECONDS);
                if (poll != null) {
                    ReferenceCountUtil.safeRelease(poll.getByteBuf());
                    continue;
                }
                // 连续两次poll都是null，确认没人再写了
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 释放session
     */
    private static void release(String id, SessionDataWrapper sessionDataWrapper) {
        if (sessionDataWrapper == null) {
            return;
        }
        if (sessionDataWrapper.isFile) {
            closeQueue(FILE_ITEM_MAP.remove(id));
        }
        SESSION_MANAGER.release(id);
    }

    /**
     * 释放session
     */
    public static void release(String id) {
        SessionDataWrapper session = SESSION_MANAGER.getSession(id);
        release(id, session);
    }

    /**
     * description
     *
     * @author yaochuang 2025/04/10 09:20
     */
    @Data
    public static class FileChunkItem {
        private ByteBuf byteBuf;
        private long buffer;  //每次传输的大小
        private long serial;  // 编号
    }

    public static void releaseFileChunk(FileChunkItem item) {
        if (item == null) {
            return;
        }
        releaseFileChunk(item.getByteBuf());
    }

    public static void releaseFileChunk(ByteBuf byteBuf) {
        if (byteBuf == null) {
            return;
        }
        if (byteBuf.refCnt() > 0) {
            ReferenceCountUtil.safeRelease(byteBuf);
        }
    }

}
