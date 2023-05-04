package org.jay.parser.impl.regex2;

public enum RepeatType {
    RANGE, //{m,n}
    REPEAT, //{n}
    MANY, //*
    SOME, //+
    OPTIONAL; //?
}
