package com.lezw.util;

/**
 * @author zhongwei.long
 * @date 2022年02月16日 14:05
 */
public class StringUtils {
    private static boolean isEmpty(final String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isBlank(final String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}