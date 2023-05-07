package org.jay.parser;

import lombok.Getter;
import org.jay.parser.comb.BacktraceParser;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.ErrorUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This is the core class of Parser Combinator.
 */
public abstract class Parser {
    protected boolean ignore = false;
    @Getter
    protected Deque<String> queue;

    public Parser(String label) {
        this.label = label;
        this.queue = new ArrayDeque<>();
        this.queue.add(label);
    }

    public Parser(String label, Deque<String> queue) {
        this.label = label;
        this.queue = queue;
        this.queue.add(label);
    }

    /**
     * Override default label
     * @param label
     * @return
     */
    public Parser label(String label) {
        this.label = label;
        this.queue.pollLast();
        this.queue.add(label);
        return this;
    }

    @Getter
    protected String label;

    public boolean isIgnore() {
        return this.ignore;
    }


    /**
     * Parse, but ignore the parsing result.
     * @return
     */
    public Parser ignore() {
        this.ignore = true;
//        this.label = String.format("EG(%s)", this.label);
        return this;
    }

    public Result runParser(IBuffer buffer) {
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
    public abstract Result parse(IBuffer buffer);

    /**
     * Connect with another parser
     * @param generator
     * @return
     */
    public Parser chainWith(Function<Result, Parser> generator) {
        return new Parser(this.label + "--", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return Result.builder().errorMsg(step1.errorMsg).build();
                }
                Result step2 = generator.apply(step1).runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return Result.builder().errorMsg(step2.errorMsg).build();
                }
                Result result = Result.empty();
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
    public Parser chain(Parser parser) {
        return new Parser(this.label + "--", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return Result.builder().errorMsg(step1.errorMsg).build();
                }
                Result step2 = parser.runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return Result.builder().errorMsg(step2.errorMsg).build();
                }
                Result result = Result.empty();
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
    public Parser chain(Supplier<Parser> parser) {
        return new Parser(this.label + "--", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return Result.builder().errorMsg(step1.errorMsg).build();
                }
                Parser theParser = parser.get();
                Result step2 = theParser.runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return Result.builder().errorMsg(step2.errorMsg).build();
                }
                Result result = Result.empty();
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
    public Parser must(Predicate<Result> p) {
        return new Parser(String.format("<%s>", this.label), this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Parser.this.runParser(buffer);
                if (result.isSuccess() && p.test(result)) {
                    return result;
                }
                return Result.builder()
                        .errorMsg(ErrorUtil.error(buffer))
                        .build();
            }
        };
    }

    /**
     * Repeat at least once
     * @return
     */
    public Parser some() {
//        return connect(() -> many());
        return new Parser(this.label + "+", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result tmp = Parser.this.runParser(buffer);
                if (tmp.isError()) {
                    return tmp;
                }
                Result result = Result.empty();
                result.length += tmp.length;
                result.addAll(tmp.getResult());
                while(true) {
                    tmp = Parser.this.runParser(buffer);
                    if (tmp.isError()) {
                        break;
                    }
                    result.length += tmp.length;
                    result.addAll(tmp.getResult());
                }
                return result;
            }
        };
    }

    /**
     * Repeat at least 0 times
     * @return
     */
    public Parser many() {
        return new Parser(this.label + "*", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Result.empty();
                Result tmp = Parser.this.runParser(buffer);
                if (tmp.isError()) {
                    return result;
                }
                result.length += tmp.length;
                result.addAll(tmp.getResult());
                while(true) {
                    tmp = Parser.this.runParser(buffer);
                    if (tmp.isError()) {
                        break;
                    }
                    result.length += tmp.length;
                    result.addAll(tmp.getResult());
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
        return repeat(from).chain(() -> attempt(end - from));
    }

    /**
     * Perform at most n times
     * @param n
     * @return
     */
    public Parser attempt(int n) {
        return new Parser(String.format("%s{0,%d}", this.label, n), this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Result.empty();
                if (n <= 0) {
                    return result;
                }
                Result tmp = Parser.this.runParser(buffer);
                if (tmp.isError()) {
                    return result;
                }
                result.length += tmp.length;
                result.addAll(tmp.getResult());
                int i = 1;
                while(i++ < n) {
                    tmp = Parser.this.runParser(buffer);
                    if (tmp.isError()) {
                        break;
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
        return new Parser(String.format("%s{%d}", this.label, n), this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Result.empty();
                if (n <= 0) {
                    return result;
                }
                Result tmp = Parser.this.runParser(buffer);
                if (tmp.isError()) {
                    return result;
                }
                result.length += tmp.length;
                result.addAll(tmp.getResult());
                int i = 1;
                while(i++ < n) {
                    tmp = Parser.this.runParser(buffer);
                    if (tmp.isError()) {
                        return tmp;
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
        return map(mapper, "?", "?");
    }

    /**
     * Map the result to another value.
     * @param mapper
     * @return
     */
    public Parser map(Function<List, ?> mapper, String from, String to) {
        return new Parser(String.format("(%s -> %s) <$> %s", from, to, this.label), this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Parser.this.runParser(buffer);
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
    public Parser trim(boolean includeNewline) {
        if (includeNewline) {
            return TextParsers.whites().chain(() -> this)
                    .chain(() -> TextParsers.whites());
        }
        return TextParsers.spaces().chain(() -> this)
                .chain(() -> TextParsers.spaces());
    }

    /**
     * Split by a specified character, the delimiter will not appear in the result.
     * @param parser
     * @return
     */
    public Parser sepBy(Parser parser) {
        return chain(() -> parser.chain(() -> this).many());
    }


    /**
     * Same as Combinater Choose
     * @param parser
     * @return
     */
    public Parser or(Supplier<Parser> parser) {
        return new Parser(this.label + "--?", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Parser.this.runParser(buffer);
                if (result.isSuccess()) {
                    return result;
                }
                Parser theParser = parser.get();
                Result result2 = theParser.runParser(buffer);
                if (result2.isSuccess()) {
                    return result2;
                }
                return Result.builder()
                        .errorMsg("No suitable Parser to choose")
                        .build();
            }
        };
    }

    /**
     * make this Parser optional
     * @return
     */
    public Parser optional() {
        return attempt(1);
    }


    /**
     * Do nothing but return success
     * @return
     */
    public static Parser empty() {
        return new Parser("TextParser.empty()") {
            @Override
            public Result parse(IBuffer buffer) {
                return Result.builder()
                        .length(0)
                        .result(new ArrayList(0))
                        .build();
            }
        };
    }

    /**
     * while(buffer.remaining()) {
     *     Result result = parser.runParser(buffer);
     * }
     * @param stripper
     * @return
     */
    public Parser scan(Supplier<Parser> stripper) {
        return new Parser(this.label + "scan", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                while(buffer.remaining() > 0) {
                    Result result = Parser.this.runParser(buffer);
                    if (result.isSuccess()) {
                        return result;
                    }
                    result = stripper.get().runParser(buffer);
                    if (result.isError()) {
                        return result;
                    }
                }
                return Result.reachEnd();
            }
        };
    }

    /**
     * a broken parser can never parse input
     * @return
     */
    public static Parser broken() {
        return new Parser("Broken", new ArrayDeque<>(1)) {
            @Override
            public Result parse(IBuffer buffer) {
                return Result.broken();
            }
        };
    }

    /**
     * Backtracking-enabled connect
     * @param  parsers
     * @return
     */
    public Parser btChain(boolean greedy, List<Supplier<Parser>> parsers) {
        return new BacktraceParser(greedy, this, parsers);
    }


    /**
     * Backtracking-enabled connect
     * @param tail
     * @return
     */
    public Parser btChain(boolean greedy, Supplier<Parser> ...tail) {
        return new BacktraceParser(greedy, this, tail);
    }

    /**
     * choose a Parser from array of Parser
     * @param parsers
     * @return
     */
    public static Parser choose(Supplier<Parser> ...parsers) {
        Parser parser = Parser.broken();
        for (Supplier<Parser> p : parsers) {
            parser = parser.or(p);
        }
        return parser;
    }

    /**
     * choose a Parser from array of Parser
     * @param parsers
     * @return
     */
    public static Parser choose(Parser ...parsers) {
        Parser parser = Parser.broken();
        for (Parser p : parsers) {
            parser = parser.or(() -> p);
        }
        return parser;
    }

    /**
     * choose a Parser from array of Parser
     * @param parsers
     * @return
     */
    public static Parser choose(List<Parser> parsers) {
        Parser parser = Parser.broken();
        for (Parser p : parsers) {
            parser = parser.or(() -> p);
        }
        return parser;
    }
}
