package org.jay.parser.util;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Mapper {

    /**
     * [Character] -&gt; String
     * @return a mapper
     */
    public static Function<List, ?> toStr() {
        return chars -> chars.stream().map(String::valueOf).collect(Collectors.joining());
    }

    /**
     * [String](1)-&gt; Int
     * @return a mapper
     */
    public static Function<List, ?> toInt() {
        return str -> {
            assert str.size() == 1;
            return Integer.parseInt(String.valueOf(str.get(0)));
        };
    }

    /**
     * List&lt;Byte&gt; -&gt; byte[]
     * @return a mapper
     */
    public static Function<List, ?> toBytes() {
        return bytes -> bytes.toArray();
    }

    /**
     * List&lt;byte[]&gt; -&gt; char
     * @param charset the charset
     * @return a mapper
     */
    public static Function<List, Character> toChar(Charset charset) {
        return bytes -> (Character) bytes.stream().map(bs ->
                CharUtil.read((byte[]) bs, charset).get()).findFirst().get();
    }

    /**
     * replace parse result with given value
     * @param value value to replace
     * @param <T> the type
     * @return a mapper
     */
    public static <T> Function<List, T> replace(T value) {
        return __ -> value;
    }

}
