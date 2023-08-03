package io.github.janlely.jparser.util;

import io.github.janlely.jparser.IBuffer;


/**
 * error message util
 */
public class ErrorUtil {

    /**
     * @param buffer the buffer
     * @return error message
     */
    public static String error(IBuffer buffer) {
        return String.format("unexpected character at: %d, char hex: %02x",
                buffer.getPos(),
                buffer.head().orElse((byte) 0));
    }
}
