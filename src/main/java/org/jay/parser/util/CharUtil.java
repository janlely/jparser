package org.jay.parser.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Optional;

/**
 * character util
 */
@Slf4j
public class CharUtil {

    /**
     * @param bytes bytes to be read
     * @param charset charset
     * @return A character or not
     */
    public static Optional<Character> read(byte[] bytes, Charset charset) {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            ByteBuffer bf = ByteBuffer.wrap(bytes);
            CharBuffer cb = CharBuffer.allocate(1);
            decoder.decode(bf, cb, true);
            return Optional.of(cb.flip().charAt(0));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
