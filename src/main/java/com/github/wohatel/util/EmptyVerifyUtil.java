package com.github.wohatel.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

/** * verify empty connection
 *
 * @author yaochuang 2025/09/17 18:00
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmptyVerifyUtil {

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }
}
