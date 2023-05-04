package org.jay.parser.impl.regex2;

import lombok.Builder;
import lombok.Data;

import java.util.function.Predicate;

@Data
@Builder
public class Token {

    TokenType type;
    Object value;

    public <T> T getValue() {
        return (T) value;
    }

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

        public <T> T getValue() {
            return (T) value;
        }
    }

}
