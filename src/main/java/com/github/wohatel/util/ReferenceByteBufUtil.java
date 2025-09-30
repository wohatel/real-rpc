package com.github.wohatel.util;

import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
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


    public static void finallyRelease(Runnable runnable, Object... inputs) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            safeRelease(inputs);
        }
    }

    public static <T> T finallyRelease(CalledAble<T> calledAble, Object... inputs) {
        try {
            return calledAble.call();
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            safeRelease(inputs);
        }
    }

    public static <T> T exceptionRelease(CalledAble<T> calledAble, Object... inputs) {
        try {
            return calledAble.call();
        } catch (Throwable throwable) {
            safeRelease(inputs);
            throw throwable;
        }
    }

    public static void exceptionRelease(Runnable runnable, Object... inputs) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            safeRelease(inputs);
            throw throwable;
        }
    }

}
