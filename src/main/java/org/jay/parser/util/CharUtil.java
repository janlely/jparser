package org.jay.parser.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class CharUtil {

    public static char read(byte[] bytes, Charset charset) throws CharacterCodingException {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            ByteBuffer bf = ByteBuffer.wrap(bytes);
            CharBuffer cb = CharBuffer.allocate(1);
            decoder.decode(bf, cb, true);
            return cb.flip().charAt(0);
        } catch (Exception e) {
            throw new CharacterCodingException();
        }
    }

}
