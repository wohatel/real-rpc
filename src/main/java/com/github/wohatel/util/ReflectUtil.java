package com.github.wohatel.util;

import java.lang.reflect.Method;

/** *
 * @author yaochuang 2025/05/13 11:54
 */
public class ReflectUtil {

    /**     * Gets the first name match method in a given class (including inherited public methods)
     */
    public static Method getFirstMethodByName(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) { // 包含继承的方法
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**     * Determine if the method is a rewrite of the default method in the interface
     */
    public static boolean isOverridingInterfaceDefaultMethod(Class<?> implClass, String methodName) {
        Method method = getFirstMethodByName(implClass, methodName);
        if (method == null) {
            return false;
        }
        // 判断该方法是否来自 implClass（而不是接口）
        return !method.getDeclaringClass().isInterface();
    }

    /**     * Determine if the method is a rewrite of the default method in the interface
     */
    public static boolean isOverridingInterfaceDefaultMethodByImplObj(Object implObj, String methodName) {
        if (implObj == null) {
            return false;
        }
        return isOverridingInterfaceDefaultMethod(implObj.getClass(), methodName);
    }

}
