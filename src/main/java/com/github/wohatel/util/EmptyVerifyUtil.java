package com.github.wohatel.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

/**
 * Utility class for verifying empty collections, maps, and arrays.
 * This class provides static utility methods to check if various data structures are empty or null.
 * The class is designed with private constructor to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmptyVerifyUtil {

    /**
     * Checks if the given Collection is either null or empty.
     *
     * @param <T>        the type of elements in the collection
     * @param collection the Collection to check
     * @return true if the collection is null or empty, false otherwise
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if the given Map is either null or empty.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param map the Map to check
     * @return true if the map is null or empty, false otherwise
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Checks if the given array is either null or empty.
     *
     * @param <T>   the type of elements in the array
     * @param array the array to check
     * @return true if the array is null or empty, false otherwise
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }
}
