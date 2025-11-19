package com.github.wohatel.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * description
 *
 * @author yaochuang 2025/11/14 17:14
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomUtil {

    public static String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String randomUUIDWithTime() {
        return System.currentTimeMillis() + ":" + randomUUID();
    }
}


