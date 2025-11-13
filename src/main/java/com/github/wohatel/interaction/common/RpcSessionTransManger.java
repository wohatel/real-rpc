package com.github.wohatel.interaction.common;

import com.github.wohatel.constant.RpcErrorEnum;
import com.github.wohatel.constant.RpcException;
import com.github.wohatel.interaction.base.RpcSession;
import com.github.wohatel.interaction.constant.RpcNumberConstant;
import com.github.wohatel.interaction.file.RpcFileReceiveWrapper;
import com.github.wohatel.util.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
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
import java.util.function.Consumer;

/**

 *
 * @author yaochuang 2025/04/10 09:25
 */
@Slf4j
public class RpcSessionTransManger {

    private static final SessionManager<SessionDataWrapper> SESSION_MANAGER = new SessionManager<>(RpcNumberConstant.K_TEN, (sessionId, wrapper) -> removeDataMap(sessionId));
    private static final Map<String, BlockingQueue<FileChunkItem>> FILE_ITEM_MAP = new ConcurrentHashMap<>();
    private static final Map<String, RpcSessionReactionWaiter> SESSION_REACTION_WAITER_MAP = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    public static class SessionDataWrapper {
        private boolean isFile;
        private RpcSessionContextWrapper contextWrapper;
    }

    public static void initSession(RpcSessionContext context, RpcSession rpcSession, ChannelHandlerContext ctx) {
        String sessionId = rpcSession.getSessionId();
        if (isRunning(sessionId)) {
            throw new RpcException(RpcErrorEnum.CONNECT, "session exists");
        }
        SessionDataWrapper sessionDataWrapper = new SessionDataWrapper(false, new RpcSessionContextWrapper(rpcSession, context));
        SESSION_MANAGER.initSession(sessionId, sessionDataWrapper, rpcSession.getTimeOutMillis() + System.currentTimeMillis());
        SESSION_REACTION_WAITER_MAP.putIfAbsent(sessionId, new RpcSessionReactionWaiter(ctx, sessionId));
    }

    public static RpcSessionReactionWaiter getWaiter(String sessionId) {
        return SESSION_REACTION_WAITER_MAP.get(sessionId);
    }

    /**
     * 注册销毁事件
     *
     * @param sessionId sessionId
     * @param consumer  消费者
     */
    public static void registOnRelease(String sessionId, Consumer<SessionDataWrapper> consumer) {
        SESSION_MANAGER.registOnRelease(sessionId, consumer);
    }

    public static boolean flush(String sessionId, long sessionTime) {
        return SESSION_MANAGER.flushTime(sessionId, sessionTime);
    }

    public static boolean flush(String sessionId) {
        RpcSession rpcSession = SESSION_MANAGER.getSession(sessionId).getContextWrapper().getRpcSession();
        if (rpcSession != null) {
            return flush(sessionId, rpcSession.getTimeOutMillis());
        }
        return false;
    }

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

    public static boolean isRunning(String sessionId) {
        return SESSION_MANAGER.contains(sessionId);
    }

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

    public static void release(String sessionId) {
        SESSION_MANAGER.release(sessionId);
        removeDataMap(sessionId);
    }

    public static void removeDataMap(String sessionId) {
        FILE_ITEM_MAP.remove(sessionId);
        SESSION_REACTION_WAITER_MAP.remove(sessionId);
    }

    @Data
    public static class FileChunkItem {
        private ByteBuf byteBuf;
        private long buffer;  //每次传输的大小
        private long serial;  // 编号
    }

}
