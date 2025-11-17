package com.github.wohatel.util;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.TypeReference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

/**
 * A utility class for JSON parsing and serialization operations.
 * This class provides methods to convert Java objects to JSON and vice versa.
 * The class is designed with a private constructor to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtil {

    /**
     * Parses an object to a specified type.
     *
     * @param <T>   the type of the object to parse to
     * @param obj   the object to parse
     * @param clazz the class of the object to parse to
     * @return the parsed object of type T, or null if the input object is null
     */
    public static <T> T parseObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return JSON.parseObject((String) obj, clazz);
        }
        return JSON.parseObject(JSON.toJSONString(obj), clazz);
    }

    /**
     * Parses a collection to a list of specified type.
     *
     * @param <T>        the type of the elements in the list
     * @param collection the collection to parse
     * @param clazz      the class of the elements in the list
     * @return the parsed list of type T, or null if the input collection is null
     */
    public static <T> List<T> parseArray(Collection<?> collection, Class<T> clazz) {
        if (collection == null) {
            return null;
        }
        return parseArray(JSONArray.toJSONString(collection), clazz);
    }

    /**
     * Parses a JSON array string to a list of specified type.
     *
     * @param <T>      the type of the elements in the list
     * @param jsonArry the JSON array string to parse
     * @param clazz    the class of the elements in the list
     * @return the parsed list of type T, or null if the input JSON array string is null
     */
    public static <T> List<T> parseArray(String jsonArry, Class<T> clazz) {
        if (jsonArry == null) {
            return null;
        }
        return JSONArray.parseArray(jsonArry, clazz);
    }

    /**
     * Converts an object to a JSON string.
     *
     * @param obj the object to convert
     * @return the JSON string representation of the object, or null if the input object is null
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * Parses a JSON string to an object of specified type.
     *
     * @param <T>   the type of the object to parse to
     * @param json  the JSON string to parse
     * @param clazz the class of the object to parse to
     * @return the parsed object of type T, or null if the input JSON string or class is null
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || clazz == null) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    /**
     * Parses a JSON string to an object of specified type using TypeReference.
     * This method is useful for parsing complex types like generics.
     *
     * @param <T> the type of the object to parse to
     * @param json the JSON string to parse
     * @param typeReference the type reference of the object to parse to
     * @return the parsed object of type T, or null if the input JSON string or type reference is null
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }
        return JSON.parseObject(json, typeReference);
    }
}
