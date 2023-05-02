package org.jay.parser.impl.regex;

public enum CharType {
    CHAR, //normal char
    ESCAPE, // \s,\S,\t...
    SELECT, // []
    DOT; // .
}
