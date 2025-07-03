package com.murong.rpc.util;

import com.alibaba.fastjson2.JSON;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * @author yaochuang
 */
public class RpcSerializer {

    /**
     * 将参数对象数组序列化成 JSON 字符串数组
     */
    public static String[] serializeParams(Object[] args) {
        if (args == null) {
            return new String[0];
        }
        return Arrays.stream(args)
                .map(JSON::toJSONString)
                .toArray(String[]::new);
    }

    /**
     * 使用 Method 对象精确反序列化 JSON 参数（支持嵌套泛型）
     */
    public static Object[] deserializeParams(Method method, String[] jsonArgs) {
        Type[] types = method.getGenericParameterTypes();
        if (types.length != jsonArgs.length) {
            throw new IllegalArgumentException("参数个数不一致");
        }

        Object[] result = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            result[i] = JSON.parseObject(jsonArgs[i], type);
        }
        return result;
    }

    /**
     * 将方法签名转成唯一字符串（含泛型信息）
     */
    public static String serialMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName())
                .append("#")
                .append(method.getName())
                .append("(");
        Type[] paramTypes = method.getGenericParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            sb.append(typeToString(paramTypes[i]));
            if (i < paramTypes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 把 Type 对象（含泛型）转成可读字符串
     */
    private static String typeToString(Type type) {
        if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            Type[] args = pt.getActualTypeArguments();
            String argsStr = Arrays.stream(args)
                    .map(RpcSerializer::typeToString)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            return ((Class<?>) raw).getName() + "<" + argsStr + ">";
        } else if (type instanceof Class<?>) {
            return ((Class<?>) type).getName();
        } else {
            return type.getTypeName();
        }
    }
}