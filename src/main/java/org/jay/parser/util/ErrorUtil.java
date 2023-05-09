package org.jay.parser.util;

import org.jay.parser.IBuffer;

/**
 * error message util
 */
public class ErrorUtil {

    /**
     * @param buffer the buffer
     * @return error message
     */
    public static String error(IBuffer buffer) {
        return String.format("unexpected character at: %d, char: %c",
                buffer.getPos(),
                buffer.head().orElse((byte) 0));
    }
}
