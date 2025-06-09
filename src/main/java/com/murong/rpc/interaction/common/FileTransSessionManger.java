package com.murong.rpc.interaction.common;

import com.murong.rpc.interaction.constant.NumberConstant;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * description
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Log
public class FileTransSessionManger {

    /**
     * 文件块的释放,需要比较久的时间,为了避免单线程造成的资源关闭堆积,才出采用线程池= true
     */
    private static final SessionManager<BlockingQueue<FileChunkItem>> FILE_SESSION_MANAGER = new SessionManager<>(NumberConstant.THREE_TEN_K, FileTransSessionManger::close);

    public static boolean addOrRelease(String sessionId, FileChunkItem fileChunkItem) {
        boolean normal = isNormal(sessionId);
        try {
            if (normal) {
                FILE_SESSION_MANAGER.getSession(sessionId).add(fileChunkItem);
                FILE_SESSION_MANAGER.flushTime(sessionId);
            } else {
                releaseFileChunk(fileChunkItem);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "文件块添加异常:", e);
            releaseFileChunk(fileChunkItem);
            return false;
        }
        return normal;
    }

    /**
     * 是否正常运行
     */
    public static boolean isNormal(String sessionId) {
        return FILE_SESSION_MANAGER.contains(sessionId);
    }

    public static void init(String sessionId) {
        init(sessionId, 100);
    }

    public static void init(String sessionId, int cacheBlock) {
        BlockingQueue<FileChunkItem> session = FILE_SESSION_MANAGER.getSession(sessionId);
        if (session != null) {
            throw new RuntimeException("文件session已存在");
        }
        PriorityBlockingQueue<FileChunkItem> queue = new PriorityBlockingQueue<>(cacheBlock, Comparator.comparingInt(t -> (int) (t.getSerial())));
        FILE_SESSION_MANAGER.initSession(sessionId, queue);
    }

    @SneakyThrows
    public static FileChunkItem poll(String sessionId, long timeMills) {
        FileChunkItem poll = FILE_SESSION_MANAGER.getSession(sessionId).poll(timeMills, TimeUnit.MILLISECONDS);
        if (poll != null) {
            FILE_SESSION_MANAGER.flushTime(sessionId);
        }
        return poll;
    }


    /**
     * 小窗口机制延迟结束
     */
    public static void close(BlockingQueue<FileChunkItem> queue) {
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
    public static void release(String id) {
        BlockingQueue<FileChunkItem> release = FILE_SESSION_MANAGER.release(id);
        FileTransSessionManger.close(release);
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
