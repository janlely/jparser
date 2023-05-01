package org.jay.parser.impl.regex;

public enum TokenType {
    CHAR, //any normal char
    ESCAPE, // \s,\S,\t...
    DOT, // .
    START, //^
    END, //$
    RANGE, //{m,n}
    REPEAT, //{n}
    MANY, //*
    SOME, //+
    OPTIONAL, //?
    SELECT, //[]
    GROUP, //()
    QUOTE, //\n
    OR, // |
}
