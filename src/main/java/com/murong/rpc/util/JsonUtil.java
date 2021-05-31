package com.murong.rpc.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

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
}
