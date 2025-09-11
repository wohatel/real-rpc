package com.github.wohatel.util;

public class OSUtils {

    public enum OSType {
        MAC, LINUX, WINDOWS, OTHER
    }

    /**
     * 区分os
     *
     * @return
     */
    public static OSType detectOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            return OSType.MAC;
        } else if (os.contains("linux")) {
            return OSType.LINUX;
        } else if (os.contains("windows")) {
            return OSType.WINDOWS;
        } else {
            return OSType.OTHER;
        }
    }
}