package org.jay.parser.util;

import lombok.Builder;
import lombok.Data;

import java.nio.charset.Charset;

@Data
@Builder
public class AnyChar {
    private byte[] character;
    private Charset charset;

    public static AnyChar from(char ch, Charset charset) {
        return AnyChar.builder()
                .character(charset.encode(String.valueOf(ch)).array())
                .charset(charset)
                .build();
    }

    public static AnyChar from(byte[] bs, Charset charset) {
        return AnyChar.builder().character(bs).charset(charset).build();
    }

    public boolean isEmpty() {
        return this.character.length == 0;
    }
    @Override
    public String toString() {
        return new String(this.character, this.charset);
    }
}
