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
    private static final SessionManager<SessionDataWrapper> SESSION_MANAGER = new SessionManager<>(NumberConstant.K_TEN, (sessionId, wrapper) -> closeQueue(sessionId));
    private static final Map<String, BlockingQueue<FileChunkItem>> FILE_ITEM_MAP = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class SessionDataWrapper {
        private boolean isFile;
        private RpcSessionContextWrapper contextWrapper;
        private String channelId;
    }

    public static void initSession(RpcSessionContext context, RpcSession rpcSession, String channelId) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.CONNECT, "session exists");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(false, new RpcSessionContextWrapper(rpcSession, context), channelId);
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
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
        RpcSession rpcSession = SESSION_MANAGER.getSession(sessionId).getContextWrapper().getRpcSession();
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
            log.error("file block - receive - print exception information:", e);
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

    public static void initFile(RpcSession rpcSession, int cacheBlock, RpcFileReceiveWrapper data, String channelId) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.HANDLE_MSG, "the file session already exists");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(true, data, channelId);
        PriorityBlockingQueue<FileChunkItem> queue = new PriorityBlockingQueue<>(cacheBlock + 1, Comparator.comparingLong(FileChunkItem::getSerial));
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        FILE_ITEM_MAP.put(sessionId, queue);
    }

    @SneakyThrows
    public static FileChunkItem poll(String sessionId, long timeMills) {
        FileChunkItem poll = FILE_ITEM_MAP.get(sessionId).poll(timeMills, TimeUnit.MILLISECONDS);
        if (poll != null) {
            SESSION_MANAGER.flushTime(sessionId);
        }
        return poll;
    }

    public static RpcSessionContextWrapper getContextWrapper(String sessionId) {
        return SESSION_MANAGER.getSession(sessionId).getContextWrapper();
    }

    /**
     * 外部清理session
     */
    public static void release(String sessionId) {
        SESSION_MANAGER.release(sessionId);
        closeQueue(sessionId);
    }

    /**
     * 外部清理session
     */
    public static void releaseFile(String sessionId) {
        SessionDataWrapper sessionWrapper = SESSION_MANAGER.getSession(sessionId);
        if (sessionWrapper != null && sessionWrapper.isFile) {
            release(sessionId);
        }
    }

    /**
     * 小窗口机制延迟结束
     */
    public static void closeQueue(String sessionId) {
        BlockingQueue<FileChunkItem> queue = FILE_ITEM_MAP.remove(sessionId);
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
