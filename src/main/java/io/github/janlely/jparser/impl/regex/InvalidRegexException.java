package io.github.janlely.jparser.impl.regex;

/**
 * invalid regex exception
 */
public class InvalidRegexException extends RuntimeException {

    /**
     * @param msg the message
     */
    public InvalidRegexException(String msg) {
        super(msg);
    }
}
