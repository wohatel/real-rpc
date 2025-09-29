package com.github.wohatel.util;

/**
 * @author yaochuang 2025/09/26 09:12
 */
@FunctionalInterface
public interface CalledAble<T> {
    T call();
}
