package org.jay.parser.impl.regex;

import lombok.Builder;
import lombok.Data;

import java.util.function.Predicate;

@Builder
public class Token {

    TokenType type;
    Object value;

    @Data
    @Builder
    public static class CharToken {
        private CharType type;
        private Predicate<Character> predicate;
    }

    @Data
    @Builder
    public static class RepeatToken {
        private RepeatType type;
        private Object value;
    }

}
