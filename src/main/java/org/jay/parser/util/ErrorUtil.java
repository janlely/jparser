package org.jay.parser.util;

public class ErrorUtil {

    public static String error(Buffer buffer) {
        return String.format("unexpected charactor at: %d, char: %c",
                buffer.getPos(),
                buffer.head().orElse((byte) 0));
    }

    public static String error(Buffer buffer, Object expected) {
        return String.format("unexpected charactor at: %d, char: %s, expected: %c",
                buffer.getPos(),
                buffer.head().orElse((byte) 0),
                expected.toString());
    }
}
