package org.jay.parser.impl.regex;

public enum RepeatType {
    RANGE, //{m,n}
    REPEAT, //{n}
    MANY, //*
    SOME, //+
    OPTIONAL; //?
}
