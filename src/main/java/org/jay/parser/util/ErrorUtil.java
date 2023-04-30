package org.jay.parser.util;

import org.jay.parser.IBuffer;

public class ErrorUtil {

    public static String error(IBuffer buffer) {
        return String.format("unexpected charactor at: %d, char: %c",
                buffer.getPos(),
                buffer.head().orElse((byte) 0));
    }

    public static String error(IBuffer buffer, Object expected) {
        return String.format("unexpected charactor at: %d, char: %s, expected: %c",
                buffer.getPos(),
                buffer.head().orElse((byte) 0),
                expected.toString());
    }
}
