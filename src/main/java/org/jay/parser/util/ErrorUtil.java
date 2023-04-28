package org.jay.parser.util;

public class ErrorUtil {

    public static String error(int pos) {
        return String.format("unexpected charactor at: %d", pos);
    }

    public static String error(int pos, Object expected) {
        return String.format("unexpected charactor at: %d, expected: %s", pos, expected.toString());
    }
}
