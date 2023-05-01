package org.jay.parser.impl.regex;

import lombok.Builder;

import java.util.function.Predicate;

@Builder
public class Token {

    TokenType type;
    Object value;

}
