package com.github.wohatel.util;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaochuang 2025/09/25 17:43
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ReferenceByteBufUtil {

    public static void safeRelease(Object... inputs) {
        if (inputs != null) {
            for (Object byteBuf : inputs) {
                try {
                    if (byteBuf instanceof ReferenceCounted ref && ref.refCnt() > 0) {
                        ReferenceCountUtil.release(ref);
                    }
                } catch (Throwable throwable2) {
                    log.warn("release buf exception:", throwable2);
                }
            }
        }
    }

}
