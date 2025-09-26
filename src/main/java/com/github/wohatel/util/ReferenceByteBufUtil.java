package com.github.wohatel.util;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import lombok.extern.slf4j.Slf4j;

/**
 * description
 *
 * @author yaochuang 2025/09/25 17:43
 */
@Slf4j
public class ReferenceByteBufUtil {

    public static void safeRelease(Object... inputs) {
        if (inputs != null) {
            for (Object byteBuf : inputs) {
                try {
                    ReferenceCountUtil.release(byteBuf);
                } catch (Throwable throwable2) {
                    log.error("release buf exception:", throwable2);
                }
            }
        }
    }

    /**
     * 最终释放
     *
     */
    public static void finallyRelease(Runnable runnable, Object... inputs) {
        try {
            runnable.run();
        } finally {
            safeRelease(inputs);
        }
    }

    /**
     * 最终释放
     *
     */
    public static <T> T finallyRelease(CalledAble<T> calledAble, Object... inputs) {
        try {
            return calledAble.call();
        } finally {
            safeRelease(inputs);
        }
    }

    /**
     * 异常释放
     */
    public static <T> T exceptionRelease(CalledAble<T> calledAble, Object... inputs) {
        try {
            return calledAble.call();
        } catch (Throwable throwable) {
            safeRelease(inputs);
            throw throwable;
        }
    }

    /**
     * 异常释放
     */
    public static void exceptionRelease(Runnable runnable, Object... inputs) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            safeRelease(inputs);
            throw throwable;
        }
    }

}
