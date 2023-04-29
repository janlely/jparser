package org.jay.parser.util;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mapper {

    //List<Character> -> String
    public static Function<List, ?> toStr() {
        return chars -> chars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    public static Function<List, ?> toBytes() {
        return bytes -> bytes.toArray();
    }

    //List<byte[]> -> char
    public static Function<List, Character> toChar(Charset charset) {
        return bytes -> (Character) bytes.stream().map(bs -> {
            try {
                return CharUtil.read((byte[]) bs, charset);
            } catch (CharacterCodingException e) {
                return null;
            }
        }).findFirst().get();
    }

    //replace parse result with given value
    public static <T> Function<List, T> replace(T value) {
        return __ -> value;
    }

}
