package org.jay.parser.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public static List<AnyChar> readN(byte[] bytes, int n, Charset charset) throws CharacterCodingException {
        try {
            CharsetDecoder decoder = charset.newDecoder();
            ByteBuffer bf = ByteBuffer.wrap(bytes);
            List<AnyChar> result = new ArrayList<>();
            int pos = 0;
            for(int i = 0; i < n; i++) {
                if (bf.remaining() <= 0) {
                    break;
                }
                CoderResult decodeRes = decoder.decode(bf, CharBuffer.allocate(1), true);
                if (decodeRes.isError()) {
                    break;
                }
                byte[] tmp = new byte[bytes.length - bf.remaining() - pos];
                System.arraycopy(bytes, pos, tmp, 0, tmp.length);
                result.add(AnyChar.builder()
                        .character(tmp)
                        .charset(charset)
                        .build());
                pos += tmp.length;
            }
            return result;
        } catch (Exception e) {
            throw new CharacterCodingException();
        }
    }

    public static int length(AnyChar ch) {
        return ch.getCharacter().length;
    }
}
