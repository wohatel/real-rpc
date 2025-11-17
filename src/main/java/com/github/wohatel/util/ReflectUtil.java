package com.github.wohatel.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Method;

/**
 * A utility class for reflection operations.
 * This class provides static methods to work with Java reflection.
 * The class is designed to be used as a utility, so it has a private constructor
 * to prevent instantiation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectUtil {

    /**     
     * Gets the first name match method in a given class (including inherited public methods)
     *
     * @param clazz The class to search for the method
     * @param methodName The name of the method to find
     * @return The first method with the given name, or null if no such method exists
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
     * Determine if the method is a rewrite of the default method in the interface
     *
     * @param implClass The implementation class to check
     * @param methodName The name of the method to check
     * @return true if the method is overriding an interface default method, false otherwise
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
     * Determine if the method is a rewrite of the default method in the interface
     * This is a convenience method that wraps isOverridingInterfaceDefaultMethod
     *
     * @param implObj The implementation object to check
     * @param methodName The name of the method to check
     * @return true if the method is overriding an interface default method, false otherwise
     */
    public static boolean isOverridingInterfaceDefaultMethodByImplObj(Object implObj, String methodName) {
        if (implObj == null) {
            return false;
        }
        return isOverridingInterfaceDefaultMethod(implObj.getClass(), methodName);
    }

}
