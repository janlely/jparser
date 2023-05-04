package org.jay.parser.impl.regex2;

public enum CharType {
    CHAR, //normal char
    ESCAPE, // \s,\S,\t...
    SELECT, // []
    DOT; // .
}
