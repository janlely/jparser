package org.jay.parser.impl.regex;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;

import java.util.function.Consumer;

/**
 * Parser with aop enabled
 */
public class AopParser extends Parser {

    private Parser parser;
    private Runnable before;
    private Consumer<Result> after;

    /**
     * @param parser The inner Parser
     * @param before What you want to do before parsing
     * @param after What you want to do before parsing
     */
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
