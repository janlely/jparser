package org.jay.parser.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mapper {

    //List<Character> -> String
    public static Function<List, ?> toStr() {
        return chars -> chars.stream().map(String::valueOf).collect(Collectors.joining());
    }
}
