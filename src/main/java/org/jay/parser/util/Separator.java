package org.jay.parser.util;

import lombok.Getter;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.ByteParsers;

public class Separator {

    @Getter
    byte[] data;
    private Separator(byte[] data) {
        this.data = data;
    }

    public static Parser character(char ch) {
        return ByteParsers.bytes(String.valueOf(ch).getBytes()).ignore();
    }

    public static Parser string(String s) {
        return ByteParsers.bytes(s.getBytes()).ignore();
    }

    public static Parser spec(byte[] data) {
        return ByteParsers.bytes(data).ignore();
    }
}
