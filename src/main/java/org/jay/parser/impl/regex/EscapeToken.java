package org.jay.parser.impl.regex;

public enum EscapeToken {

    WORD, //\w [A-z0-9_]
    NON_WORD, //\W [^A-z0-9]
    DIGIT, //\d [0-9]
    NON_DIGIT, //\D [^0-9]
    WHITE, //\s [ \t\r\n\v\f]
    NON_WHITE, //\S [^ \t\r\n\v\f]
    DOT, // \. .
    PLUS, // \+ +
    STAR, // \* *
    QUESTION_MARK, //\? ?
    LEFT_BRACKET, // \(
    RIGHT_BRACKET, // \)
    LEFT_SQUARE_BRACKET, // \[
    RIGHT_SQUARE_BRACKET, // \]
    BACKSLASH, // \\
}
