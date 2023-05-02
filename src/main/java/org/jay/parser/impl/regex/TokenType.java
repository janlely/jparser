package org.jay.parser.impl.regex;

public enum TokenType {
    VALID_CHAR, // valid char
    REPEAT, // +*?
    START, //^
    END, //$
    GROUP, //()
    QUOTE, //\n
    OR; // |
}
