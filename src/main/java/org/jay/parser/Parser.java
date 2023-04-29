package org.jay.parser;

import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.ErrorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This is the core class of Parser Combinator.
 */
public abstract class Parser {
    protected boolean ignore = false;

    public boolean isIgnore() {
        return this.ignore;
    }

    /**
     * Parse, but ignore the parsing result.
     * @return
     */
    public Parser ignore() {
        this.ignore = true;
        return this;
    }

    public Result runParser(Buffer buffer) {
        Result result = parse(buffer);
        if (result.isError()) {
            return result;
        }
        if (isIgnore()) {
            result.clear();
            return result;
        }
        return result;
    }
    public abstract Result parse(Buffer buffer);

    /**
     * Connect with another parser
     * @param generator
     * @return
     */
    public Parser connectWith(Function<Result, Parser> generator) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return Result.builder().errorMsg(step1.errorMsg).build();
                }
                Result step2 = generator.apply(step1).runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return Result.builder().errorMsg(step2.errorMsg).build();
                }
                Result result = Result.builder().result(new ArrayList()).length(0).build();
                result.length += step1.length + step2.length;
                result.addAll(step1.getResult());
                result.addAll(step2.getResult());
                return result;
            }
        };
    }


    /**
     * Connect with another parser
     * @param parser
     * @return
     */
    public Parser connect(Supplier<Parser> parser) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return Result.builder().errorMsg(step1.errorMsg).build();
                }
                Result step2 = parser.get().runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return Result.builder().errorMsg(step2.errorMsg).build();
                }
                Result result = Result.builder().result(new ArrayList()).length(0).build();
                result.length += step1.length + step2.length;
                result.addAll(step1.getResult());
                result.addAll(step2.getResult());
                return result;
            }
        };
    }

    /**
     * Add a conditional judgment
     * @param p
     * @return
     */
    public Parser must(Predicate p) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                int pos = buffer.getPos();
                Result result = Parser.this.runParser(buffer);
                if (result.isSuccess() && p.test(result.getResult())) {
                    return result;
                }
                buffer.jump(pos);
                return Result.builder()
                        .errorMsg(ErrorUtil.error(pos))
                        .build();
            }
        };
    }

    /**
     * Repeat at least once
     * @return
     */
    public Parser some() {
        return connect(() -> many());
    }

    /**
     * Repeat at least 0 times
     * @return
     */
    public Parser many() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = Result.builder().result(new ArrayList(0)).length(0).build();
                Result first = Parser.this.runParser(buffer);
                if (first.isError()) {
                    return result;
                }
                result.length += first.length;
                result.addAll(first.getResult());
                Result tmp = Parser.this.runParser(buffer);
                while(tmp.isSuccess()) {
                    result.length += tmp.length;
                    result.addAll(tmp.getResult());
                    tmp = Parser.this.runParser(buffer);
                }
                return result;
            }
        };
    }

    /**
     * The repetition count depends on the given range
     * @param from
     * @param end
     * @return
     */
    public Parser range(int from, int end) {
        return repeat(from).attempt(end - from);
    }
    public Parser attempt(int n) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = Result.empty();
                if (n <= 0) {
                    return result;
                }
                for (int i = 0; i < n; i++) {
                    Result tmp = Parser.this.runParser(buffer);
                    if (tmp.isError()) {
                        return result;
                    }
                    result.length += tmp.length;
                    result.addAll(tmp.getResult());
                }
                return result;
            }
        };
    }

    /**
     * Repeat a specified number of times.
     * @param n
     * @return
     */
    public Parser repeat(int n) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = Result.builder().result(new ArrayList(1)).length(0).build();
                if (n <= 0) {
                    return Result.builder().length(0).result(new ArrayList(0)).build();
                }
                for(int i = 0; i < n; i++) {
                    Result tmp = Parser.this.runParser(buffer);
                    if (tmp.isError()) {
                        return Result.builder().errorMsg(tmp.errorMsg).build();
                    }
                    result.length += tmp.length;
                    result.addAll(tmp.getResult());
                }
                return result;
            }
        };
    }

    /**
     * Map the result to another value.
     * @param mapper
     * @return
     */
    public Parser map(Function<List, ?> mapper) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = Parser.this.parse(buffer);
                if (result.isError()) {
                    return result;
                }
                result.map(mapper);
                return result;
            }
        };
    }

    /**
     * Trim leading and trailing whitespace.
     * @return
     */
    public Parser trim() {
        return TextParsers.blank().connect(() -> this).connect(() -> TextParsers.blank());
    }

    /**
     * Split by a specified character, the delimiter will not appear in the result.
     * @param parser
     * @return
     */
    public Parser sepBy(Parser parser) {
        return connect(() -> parser.connect(() -> this).many());
    }


    /**
     * Same as Combinater Choose
     * @param parser
     * @return
     */
    public Parser or(Supplier<Parser> parser) {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = Parser.this.runParser(buffer);
                if (result.isSuccess()) {
                    return result;
                }
                Result result2 = parser.get().runParser(buffer);
                if (result2.isSuccess()) {
                    return result2;
                }
                return Result.builder()
                        .errorMsg("No suitable Parser to choose")
                        .build();
            }
        };
    }

    public Parser optional() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = Parser.this.runParser(buffer);
                if (result.isError()) {
                    return Result.empty();
                }
                return result;
            }
        };
    }

}
