package com.github.wohatel.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *
 * @author yaochuang 2025/09/12 15:21
 */
@Data
@AllArgsConstructor
public class KeyValue<K, T> {
    private K key;
    private T value;
}
