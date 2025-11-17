package com.github.wohatel.util;

import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Utility class for handling ReferenceByteBuf operations.
 * This class provides a method to safely release ReferenceCounted objects.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ReferenceByteBufUtil {

    /**
     * Safely releases ReferenceCounted objects.
     * This method iterates through all input objects and attempts to release them
     * if they implement ReferenceCounted interface and have a reference count greater than 0.
     * Any exceptions during the release process are caught and logged.
     *
     * @param inputs Variable number of objects that might need to be released
     */
    public static void safeRelease(Object... inputs) {
        if (inputs != null) {
            for (Object byteBuf : inputs) {
                try {
                    // Check if the object is an instance of ReferenceCounted and has active references
                    if (byteBuf instanceof ReferenceCounted ref && ref.refCnt() > 0) {
                        // Release the reference using ReferenceCountUtil
                        ReferenceCountUtil.release(ref);
                    }
                } catch (Throwable throwable2) {
                    // Log any exceptions that occur during the release process
                    log.warn("release buf exception:", throwable2);
                }
            }
        }
    }

}
