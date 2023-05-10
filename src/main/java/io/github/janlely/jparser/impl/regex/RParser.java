package io.github.janlely.jparser.impl.regex;

import lombok.Builder;
import lombok.Data;
import io.github.janlely.jparser.Parser;

import java.util.function.Function;

/**
 * a Regex Parser, include some extra info
 */
@Data
@Builder
public class RParser {

    /**
     * ParserType
     */
    private ParserType type;
    /**
     * quote id if it's a QUOTE
     */
    private int quoteId;
    /**
     * group id if it's a GROUP
     */
    private int groupId;
    /**
     * the Parser equal to regex token
     */
    private Parser parser;
    /**
     * function to apply before parse
     */
    private Function<Parser, Parser> func;


    /**
     * @param func the function
     * @return this
     */
    public RParser apply(Function<Parser, Parser> func) {
        if (this.parser != null) {
            this.parser = func.apply(this.parser);
        }
        this.func = func;
        return this;
    }

    /**
     * type of parser
     */
    public enum ParserType {
        /**
         * PARSER
         */
        PARSER,
        /**
         * QUOTE
         */
        QUOTE,
        /**
         * GROUP
         */
        GROUP,
        /**
         * START
         */
        START;
    }
}
