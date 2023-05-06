package org.jay.parser.impl.regex;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;

import java.util.function.Consumer;

public class AopParser extends Parser {

    private Parser parser;
    private Runnable before;
    private Consumer<Result> after;

    public AopParser(Parser parser, Runnable before, Consumer<Result> after) {
        super(parser.getLabel(), parser.getQueue());
        this.parser = parser;
        this.before = before;
        this.after = after;
    }
    @Override
    public Result parse(IBuffer buffer) {
        before.run();
        Result result = parser.runParser(buffer);
        after.accept(result);
        return result;
    }

}
