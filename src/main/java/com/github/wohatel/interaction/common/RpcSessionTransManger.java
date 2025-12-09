package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.util.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * RpcSessionTransManger is responsible for managing RPC sessions, including session initialization,
 * file chunk handling, and session lifecycle management. It provides thread-safe operations for
 * session data and file transfers.
 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RpcSessionTransManger {

    // Session manager with custom timeout handling
    private static final SessionManager<SessionDataWrapper> SESSION_MANAGER = new SessionManager<>(RpcNumberConstant.K_TEN, (sessionId, wrapper) -> removeDataMap(sessionId));
    // Thread-safe map to store file chunk queues for each session
    private static final Map<String, BlockingQueue<FileChunkItem>> FILE_ITEM_MAP = new ConcurrentHashMap<>();
    // Thread-safe map to store session reaction waiters
    private static final Map<String, RpcSessionReactionWaiter> SESSION_REACTION_WAITER_MAP = new ConcurrentHashMap<>();

    /**
     * Wrapper class for session data containing file status and context information.
     */
    @Data
    @AllArgsConstructor
    public static class SessionDataWrapper {
        private boolean isFile; // Indicates if the session is for file transfer
        private RpcSessionContextWrapper contextWrapper; // Contains RPC session context
    }

    /**
     * Initializes a new RPC session.
     *
     * @param context    RPC session context
     * @param rpcSession RPC session object
     * @param ctx        Channel handler context
     * @throws RpcException if session already exists
     */
    public static void initSession(RpcSessionContext context, RpcSession rpcSession, ChannelHandlerContext ctx) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.CONNECT, "session exists");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(false, new RpcSessionContextWrapper(rpcSession, context));
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        SESSION_REACTION_WAITER_MAP.putIfAbsent(sessionId, new RpcSessionReactionWaiter(ctx, sessionId));
    }

    /**
     * Gets the session reaction waiter for a given session ID.
     *
     * @param sessionId The session identifier
     * @return RpcSessionReactionWaiter instance
     */
    public static RpcSessionReactionWaiter getWaiter(String sessionId) {
        return SESSION_REACTION_WAITER_MAP.get(sessionId);
    }

    /**
     * Registers a consumer to be called when the session is released.
     * @param sessionId The session identifier
     * @param runnable Consumer to be called on session release
     */
    public static void onRelease(String sessionId, Runnable runnable) {
        SESSION_MANAGER.onRelease(sessionId, runnable);
    }

    /**
     * Flushes the session timeout time.
     *
     * @param sessionId   The session identifier
     * @param sessionTime New timeout time
     * @return true if successful, false otherwise
     */
    public static boolean flush(String sessionId, long sessionTime) {
        return SESSION_MANAGER.flushTime(sessionId, sessionTime);
    }

    /**
     * Flushes the session timeout time using the session's configured timeout.
     *
     * @param sessionId The session identifier
     * @return true if successful, false otherwise
     */
    public static boolean flush(String sessionId) {
        RpcSession rpcSession = SESSION_MANAGER.getSession(sessionId).getContextWrapper().getRpcSession();
        if (rpcSession != null) {
            return flush(sessionId, rpcSession.getTimeOutMillis());
        }
        return false;
    }

    /**
     * Adds a file chunk to the session's file queue.
     *
     * @param sessionId     The session identifier
     * @param fileChunkItem The file chunk to add
     * @return true if successful, false otherwise
     */
    public static boolean addFileChunk(String sessionId, FileChunkItem fileChunkItem) {
        try {
            SessionDataWrapper session = SESSION_MANAGER.getSession(sessionId);
            if (session.isFile) {
                FILE_ITEM_MAP.get(sessionId).add(fileChunkItem);
                SESSION_MANAGER.flushTime(sessionId);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("file block - receive - print exception information:", e);
            return false;
        }
    }

    /**
     * Checks if a session is currently running.
     *
     * @param sessionId The session identifier
     * @return true if session is running, false otherwise
     */
    public static boolean isRunning(String sessionId) {
        return SESSION_MANAGER.contains(sessionId);
    }

    /**
     * Initializes a file transfer session.
     *
     * @param rpcSession RPC session object
     * @param cacheBlock Number of cache blocks
     * @param data       File receive wrapper
     * @throws RpcException if session already exists
     */
    public static void initFile(RpcSession rpcSession, int cacheBlock, RpcFileReceiveWrapper data) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.HANDLE_MSG, "the file session already exists");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(true, data);
        PriorityBlockingQueue<FileChunkItem> queue = new PriorityBlockingQueue<>(cacheBlock + 1, Comparator.comparingLong(FileChunkItem::getSerial));
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        FILE_ITEM_MAP.put(sessionId, queue);
    }

    /**
     * Polls a file chunk from the session's queue with timeout.
     *
     * @param sessionId The session identifier
     * @param timeMills Timeout in milliseconds
     * @return FileChunkItem if available, null otherwise
     */
    @SneakyThrows
    public static FileChunkItem poll(String sessionId, long timeMills) {
        FileChunkItem poll = FILE_ITEM_MAP.get(sessionId).poll(timeMills, TimeUnit.MILLISECONDS);
        if (poll != null) {
            SESSION_MANAGER.flushTime(sessionId);
        }
        return poll;
    }

    /**
     * Gets the context wrapper for a session.
     * @param sessionId The session identifier
     * @return RpcSessionContextWrapper instance
     */
    public static RpcSessionContextWrapper getContextWrapper(String sessionId) {
        return SESSION_MANAGER.getSession(sessionId).getContextWrapper();
    }

    /**
     * Releases a session and cleans up associated data.
     * @param sessionId The session identifier
     */
    public static void release(String sessionId) {
        SESSION_MANAGER.release(sessionId);
        removeDataMap(sessionId);
    }

    /**
     * Removes session-specific data maps.
     * @param sessionId The session identifier
     */
    public static void removeDataMap(String sessionId) {
        FILE_ITEM_MAP.remove(sessionId);
        SESSION_REACTION_WAITER_MAP.remove(sessionId);
    }

    /**
     * Represents a file chunk with buffer, buffer size, and serial number.
     */
    @Data
    public static class FileChunkItem {
        private ByteBuf byteBuf; // Buffer containing file data
        private long buffer;  //每次传输的大小
        private long serial;  // 编号
    }

}
