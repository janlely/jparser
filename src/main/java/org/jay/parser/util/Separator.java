package org.jay.parser.util;

import lombok.Getter;

public class Separator {

    @Getter
    byte[] data;
    private Separator(byte[] data) {
        this.data = data;
    }

    public static Separator character(char ch) {
        return new Separator(String.valueOf(ch).getBytes());
    }

    public static Separator string(String s) {
        return new Separator(s.getBytes());
    }

    public static Separator spec(byte[] data) {
        return new Separator(data);
    }
}
