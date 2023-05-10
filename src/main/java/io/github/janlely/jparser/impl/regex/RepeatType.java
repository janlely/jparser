package io.github.janlely.jparser.impl.regex;

/**
 * the repeat type
 */
public enum RepeatType {
    /**
     * range
     */
    RANGE, //{m,n}
    /**
     * repeat
     */
    REPEAT, //{n}
    /**
     * many
     */
    MANY, //*
    /**
     * some
     */
    SOME, //+
    /**
     * optional
     */
    OPTIONAL; //?
}
