package org.jay.parser.impl.regex2;

import lombok.Builder;
import lombok.Data;
import org.jay.parser.Parser;

import java.util.function.Function;

@Data
@Builder
public class RParser {

    private ParserType type;
    private int quoteId;
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
        PARSER,QUOTE,GROUP;
    }
}
