package com.github.wohatel.util;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.TypeReference;

import java.util.Collection;
import java.util.List;

public class JsonUtil {

    public static <T> T parseObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return JSON.parseObject((String) obj, clazz);
        }
        return JSON.parseObject(JSON.toJSONString(obj), clazz);
    }

    public static <T> List<T> parseArray(Collection<?> collection, Class<T> clazz) {
        if (collection == null) {
            return null;
        }
        return parseArray(JSONArray.toJSONString(collection), clazz);
    }

    public static <T> List<T> parseArray(String jsonArry, Class<T> clazz) {
        if (jsonArry == null) {
            return null;
        }
        return JSONArray.parseArray(jsonArry, clazz);
    }

    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }
        return JSON.parseObject(json, typeReference);
    }
}
