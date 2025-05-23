package com.murong.rpc.util;


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

    public static <T> List<T> parseArray(Collection collection, Class<T> clazz) {
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

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param obj 要序列化的对象
     * @return JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    /**
     * 将 JSON 字符串反序列化为带复杂泛型的对象
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @return 反序列化后的对象
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }
        return JSON.parseObject(json, typeReference);
    }
}
