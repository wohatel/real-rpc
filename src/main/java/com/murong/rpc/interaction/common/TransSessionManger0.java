package com.murong.rpc.interaction.common;

import com.murong.rpc.interaction.base.RpcSession;
import com.murong.rpc.interaction.constant.NumberConstant;
import com.murong.rpc.interaction.file.RpcFileContext;
import com.murong.rpc.interaction.file.RpcFileLocalWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * description
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Log
public class TransSessionManger0 {

    /**
     * 文件块的释放,需要比较久的时间,为了避免单线程造成的资源关闭堆积,才出采用线程池= true
     */
    private static final SessionManager<Boolean> SESSION_MANAGER = new SessionManager<>(NumberConstant.TEN, TransSessionManger0::release);
    private static final Map<String, BlockingQueue<FileChunkItem>> FILE_ITEM_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Object> SESSION_DATA = new ConcurrentHashMap<>();
    private static final Map<String, RpcSession> SESSION = new ConcurrentHashMap<>();

    /**
     * 是否正常运行
     */
    public static RpcSessionContext getSessionContext(String sessionId) {
        return (RpcSessionContext) SESSION_DATA.get(sessionId);
    }

    /**
     * 初始化
     *
     * @param sessionId
     * @param context
     */
    public static void initSession(String sessionId, RpcSessionContext context, RpcSession rpcSession) {
        if (isRunning(sessionId)) {
            throw new RuntimeException("session已存在");
        }
        SESSION_MANAGER.initSession(sessionId, false, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        if (context != null) {
            SESSION_DATA.put(sessionId, context);
        }
        SESSION.put(sessionId, rpcSession);
    }

    /**
     * 刷新时间
     *
     * @param sessionId
     * @param sessionTime
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
        Boolean isFile = SESSION_MANAGER.getSession(sessionId);
        boolean isNormal = isFile != null && isFile;
        try {
            if (isNormal) {
                FILE_ITEM_MAP.get(sessionId).add(fileChunkItem);
                SESSION_MANAGER.flushTime(sessionId);
            } else {
                releaseFileChunk(fileChunkItem);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "文件块添加异常:", e);
            releaseFileChunk(fileChunkItem);
            return false;
        }
        return isNormal;
    }

    /**
     * 是否正常运行
     */
    public static boolean isRunning(String sessionId) {
        return SESSION_MANAGER.contains(sessionId);
    }

    public static void initFile(String sessionId, int cacheBlock, Triple<RpcFileContext, RpcFileLocalWrapper, Channel> data, RpcSession rpcSession) {
        if (isRunning(sessionId)) {
            throw new RuntimeException("文件session已存在");
        }
        PriorityBlockingQueue<FileChunkItem> queue = new PriorityBlockingQueue<>(cacheBlock, Comparator.comparingInt(t -> (int) (t.getSerial())));
        SESSION_MANAGER.initSession(sessionId, true, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        FILE_ITEM_MAP.put(sessionId, queue);
        if (data != null) {
            SESSION_DATA.put(sessionId, data);
        }
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

    public static Triple<RpcFileContext, RpcFileLocalWrapper, Channel> getFileData(String sessionId) {
        return (Triple<RpcFileContext, RpcFileLocalWrapper, Channel>) SESSION_DATA.get(sessionId);
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
    public static void release(String id, Boolean isFile) {
        if (isFile != null && isFile) {
            closeQueue(FILE_ITEM_MAP.remove(id));
        }
        SESSION_DATA.remove(id);
    }

    /**
     * 释放session
     */
    public static void release(String id) {
        Boolean isFile = SESSION_MANAGER.getSession(id);
        release(id, isFile);
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
        private long length;  //文件总大小
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
