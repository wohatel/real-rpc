package com.murong.rpc.util;

import java.lang.reflect.Method;

/**
 * description
 *
 * @author yaochuang 2025/05/13 11:54
 */
public class ReflectUtil {

    /**
     * 获取指定类中第一个名称匹配的方法（包括继承的 public 方法）
     */
    public static Method getFirstMethodByName(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) { // 包含继承的方法
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * 判断该方法是否是对接口中 default 方法的重写
     */
    public static boolean isOverridingInterfaceDefaultMethod(Class<?> implClass, String methodName) {
        Method method = getFirstMethodByName(implClass, methodName);
        if (method == null) {
            return false;
        }
        // 判断该方法是否来自 implClass（而不是接口）
        return !method.getDeclaringClass().isInterface();
    }

    /**
     * 判断该方法是否是对接口中 default 方法的重写
     */
    public static boolean isOverridingInterfaceDefaultMethodByImplObj(Object implObj, String methodName) {
        if (implObj == null) {
            return false;
        }
        return isOverridingInterfaceDefaultMethod(implObj.getClass(), methodName);
    }

}
