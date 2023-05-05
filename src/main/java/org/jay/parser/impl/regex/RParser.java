package org.jay.parser.impl.regex;

import lombok.Builder;
import lombok.Data;
import org.jay.parser.Parser;

import java.util.function.Function;

/**
 * a Regex Parser, include some extra info
 */
@Data
@Builder
public class RParser {

    private ParserType type;
    private int quoteId;
    private int groupId;
    private Parser parser;
    private Function<Parser, Parser> func;


    public RParser apply(Function<Parser, Parser> func) {
        if (this.parser != null) {
            this.parser = func.apply(this.parser);
        }
        this.func = func;
        return this;
    }
    public enum ParserType {
        PARSER,QUOTE,GROUP,START;
    }
}
