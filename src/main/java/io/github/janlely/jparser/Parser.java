package io.github.janlely.jparser;

import io.github.janlely.jparser.comb.BacktraceParser;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.ErrorUtil;
import lombok.Getter;

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
    /**
     * if is ignored
     */
    protected boolean ignore = false;
    /**
     * label queue
     */
    @Getter
    protected Deque<String> queue;

    /**
     * @param label the label
     */
    public Parser(String label) {
        this.label = label;
        this.queue = new ArrayDeque<>();
        this.queue.add(label);
    }

    /**
     * @param label the label
     * @param queue the queue
     */
    public Parser(String label, Deque<String> queue) {
        this.label = label;
        this.queue = queue;
        this.queue.add(label);
    }

    /**
     * Override default label
     * @param label Indicate what the current parser is composed of.
     * @return Just return this
     */
    public Parser label(String label) {
        this.label = label;
        this.queue.pollLast();
        this.queue.add(label);
        return this;
    }

    /**
     * the label
     */
    @Getter
    protected String label;

    /**
     * @return if is ignored
     */
    public boolean isIgnore() {
        return this.ignore;
    }


    /**
     * Parse, but ignore the parsing result.
     * @return Return this
     */
    public Parser ignore() {
        this.ignore = true;
        return this;
    }

    /**
     * @param buffer the input
     * @return parser result
     */
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

    /**
     * The core function of a Parser
     * @param buffer The input
     * @return The parse result
     */
    public abstract Result parse(IBuffer buffer);

    /**
     * Connect with another parser
     * @param generator A function that takes the result of a previous parser and generates a new parser.
     * @return A new parser that is composed of the specified parser.
     */
    public Parser chainWith(Function<Result, Parser> generator) {
        return new Parser(this.label + "--", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return step1;
                }
                Result step2 = generator.apply(step1).runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return step2;
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
     * Strict Mode
     * @param parser The Parser to chain
     * @return A new parser that is composed of the specified parser.
     */
    public Parser chain(Parser parser) {
        return new Parser(this.label + "--", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return step1;
                }
                Result step2 = parser.runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return step2;
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
     * Lazy Mode
     * @param parser The Parser generator to chain
     * @return A new parser that is composed of the specified parser.
     */
    public Parser chain(Supplier<Parser> parser) {
        return new Parser(this.label + "--", this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result step1 = Parser.this.runParser(buffer);
                if (step1.isError()) {
                    return step1;
                }
                Parser theParser = parser.get();
                Result step2 = theParser.runParser(buffer);
                if (step2.isError()) {
                    buffer.backward(step1.length);
                    return step2;
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
     * @param predicate A requirement that the parsing result must satisfy.
     * @return A new parser that includes this condition check.
     */
    public Parser must(Predicate<Result> predicate) {
        return new Parser(String.format("<%s>", this.label), this.queue) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Parser.this.runParser(buffer);
                if (result.isError()) {
                    return result;
                }
                if (predicate.test(result)) {
                    return result;
                }
                buffer.backward(result.length);
                return Result.builder()
                        .errorMsg("Assertion condition not satisfied")
                        .pos(buffer.getPos())
                        .build();
            }
        };
    }

    /**
     * Repeat at least once
     * @return A new parser that will execute the current parser one or infinite times.
     */
    public Parser some() {
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
     * @return A new parser that will execute the current parser zero or infinite times.
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
     * @param from Minimum repetition count.
     * @param end Maximum repetition count.
     * @return A new parser that will execute the current parser {from} to {end} times.
     */
    public Parser range(int from, int end) {
        return repeat(from).chain(() -> attempt(end - from));
    }

    /**
     * Perform at most n times
     * @param n Maximum repetition count.
     * @return A new parser that will execute the current parser at most {n} times.
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
     * @param n Repetition count.
     * @return A new parser that will execute the current parser {n} times.
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
     * @param mapper The mapper
     * @return A new parser that is composed of the mapper.
     */
    public Parser map(Function<List, ?> mapper) {
        return map(mapper, "?", "?");
    }

    /**
     * Map the result to another value.
     * @param mapper The mapper
     * @param from The type before mapping
     * @param to  The type after mapping
     * @return A new parser that is composed of the mapper.
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
     * @param includeNewline true if newline need to be trimmed
     * @return A new Parser that is composed of the trim.
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
     * @param parser The separator Parser
     * @return A new Parser that is composed of the separator.
     */
    public Parser sepBy(Parser parser) {
        return chain(() -> parser.chain(() -> this).many());
    }


    /**
     * Same as Combinator Choose
     * @param parser A Parser generator
     * @return A new Parser that is composed of the specific Parser generator
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
                        .pos(buffer.getPos())
                        .build();
            }
        };
    }

    /**
     * make this Parser optional
     * @return A new Parser
     */
    public Parser optional() {
        return attempt(1);
    }


    /**
     * Do nothing but return success
     * @return A empty Parser
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
     * Continuously reduce the input and attempt to parse it.
     * @param stripper The strip Parser
     * @return A new Parser will continuously reduce the input and attempt to parse it.
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
     * A broken Parser can never parse input
     * @return A Broken Parser
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
     * @param greedy Greedy or Non-greedy mode
     * @param parsers Parsers to be chained
     * @return A new Parser that is composed of the parsers
     */
    public Parser btChain(boolean greedy, List<Supplier<Parser>> parsers) {
        return new BacktraceParser(greedy, this, parsers);
    }


    /**
     * Backtracking-enabled connect
     * @param greedy Greedy or Non-greedy mode
     * @param  parsers Parsers to be chained
     * @return A new Parser that is composed of the parsers
     */
    public Parser btChain(boolean greedy, Supplier<Parser> ...parsers) {
        return new BacktraceParser(greedy, this, parsers);
    }

    /**
     * choose a Parser from array of Parser
     * @param parsers Parsers candidates
     * @return A new Parser that is composed of the parsers
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
     * @param parsers Parsers candidates
     * @return A new Parser that is composed of the parsers
     */
    public static Parser choose(List<Supplier<Parser>> parsers) {
        Parser parser = Parser.broken();
        for (Supplier<Parser> p : parsers) {
            parser = parser.or(p);
        }
        return parser;
    }

    /**
     * chain a Parser from array of Parser
     * @param parsers Parsers to be chained
     * @return A new Parser that is composed of the parsers
     */
    public static Parser chains(Supplier<Parser> ...parsers) {
        Parser parser = Parser.empty();
        for (Supplier<Parser> p : parsers) {
            parser = parser.chain(p);
        }
        return parser;
    }

    /**
     * chain a Parser from array of Parser
     * @param parsers Parsers to be chained
     * @return A new Parser that is composed of the parsers
     */
    public static Parser chains(List<Supplier<Parser>> parsers) {
        Parser parser = Parser.empty();
        for (Supplier<Parser> p : parsers) {
            parser = parser.or(p);
        }
        return parser;
    }
}
