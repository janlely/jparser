package org.jay.parser.impl.regex;

import lombok.Builder;
import org.jay.parser.Parser;

import java.util.function.Predicate;

@Builder
public class Token {

    TokenType type;
    Object value;

    @Builder
    public static class CharToken {
        private CharType type;
        private Predicate<Character> predicate;
    }

    @Builder
    public static class RepeatToken {
        private RepeatType type;
        private Object value;
    }

}
