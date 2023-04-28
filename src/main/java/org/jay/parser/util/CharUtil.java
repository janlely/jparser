package org.jay.parser.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;

public class CharUtil {

    public static final int CHAR_MAN_LENGTH = 4;

    public static AnyChar read(byte[] bytes, Charset charset) throws CharacterCodingException {
        try {
            int maxLen = bytes.length > CHAR_MAN_LENGTH ? CHAR_MAN_LENGTH : bytes.length;
            CharsetDecoder decoder = charset.newDecoder();
            ByteBuffer bf = ByteBuffer.wrap(bytes, 0, maxLen);
            decoder.decode(bf, CharBuffer.allocate(1), true);
            return AnyChar.builder()
                    .character(Arrays.copyOf(bytes, maxLen - bf.remaining()))
                    .charset(charset)
                    .build();
        } catch (Exception e) {
            throw new CharacterCodingException();
        }
    }

    public static int length(AnyChar ch) {
        return ch.getCharacter().length;
    }
}
