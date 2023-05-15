package io.github.janlely.jparser;

import io.github.janlely.jparser.comb.BacktraceParser;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.ErrorUtil;

import java.util.ArrayList;
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
        return new Parser() {
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
        return new Parser() {
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
        return new Parser() {
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
        return new Parser() {
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
        return chain(() -> many());
    }

    /**
     * Repeat at least 0 times
     * @return A new parser that will execute the current parser zero or infinite times.
     */
    public Parser many() {
        return new Parser() {
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
     * repeat this till parser success
     * @param parser the Stop-parser
     * @return A new Parser
     */
    public Parser manyTill(Parser parser) {
        return manyTill(parser, false);
    }

    /**
     * repeat this till parser success
     * @param parser the Stop-Parser
     * @param greedy if is greedy mode
     * @return A new Parser
     */
    public Parser manyTill(Parser parser, boolean greedy) {
        return repeatTill(() -> parser, 0, Integer.MAX_VALUE, greedy, false);
    }

    /**
     * repeat this till parser success
     * @param parser the Stop-Parser
     * @return A new Parser
     */
    public Parser someTill(Supplier<Parser> parser) {
        return someTill(parser, false);
    }

    /**
     * repeat this till parser success
     * @param parser the Stop-Parser
     * @param greedy if is greedy mode
     * @return A new Parser
     */
    public Parser someTill(Supplier<Parser> parser, boolean greedy) {
        return this.chain(() -> manyTill(parser.get(), greedy));
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
        return new Parser() {
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
        return new Parser() {
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
     * @param parser the Stop-Parser
     * @param least repeat at least
     * @param most repeat at most
     * @return A new Parser
     */
    public Parser repeatTill(Supplier<Parser> parser, int least, int most) {
        return repeatTill(parser, least, most, false, false);
    }

    /**
     * @param parser the Stop-Parser
     * @param least repeat at least
     * @param most repeat at most
     * @param greedy if is greedy
     * @return A new Parser
     */
    public Parser repeatTill(Supplier<Parser> parser, int least, int most, boolean greedy) {
        return repeatTill(parser, least, most, greedy, false);
    }
    /**
     * @param parser the Stop-Parser
     * @param least repeat at least
     * @param most repeat at most
     * @param greedy if is greedy
     * @param keepStopResult if keep the result of Stop-Parser
     * @return A new Parser
     */
    public Parser repeatTill(Supplier<Parser> parser, int least, int most, boolean greedy, boolean keepStopResult) {
        return new Parser() {
            @Override
            public Result parse(IBuffer buffer) {
                int orgPos = buffer.getPos();
                Result repeatResult = Parser.this.repeat(least).runParser(buffer);
                if (repeatResult.isError()) {
                    return repeatResult;
                }
                Parser stopParser = parser.get();
                Result bestLeftResult = null;
                Result bestStopResult = null;
                Result currentParsedResult = repeatResult.copy();
                int n = least;
                while (n++ <= most) {
                    Result stopResult = stopParser.runParser(buffer);
                    if (stopResult.isSuccess()) {
                        if (bestLeftResult == null|| bestLeftResult.getLength() + bestStopResult.length < currentParsedResult.length + stopResult.getLength()) {
                            bestLeftResult = currentParsedResult.copy();
                            bestStopResult = stopResult;
                        }
                    }
                    if (stopResult.isSuccess() && !greedy) {
                        break;
                    }
                    if (n > most) {
                        break;
                    }
                    buffer.backward(stopResult.length);
                    Result result = Parser.this.runParser(buffer);
                    if (result.isError()) {
                        break;
                    }
                    currentParsedResult.addAll(result.getResult());
                    currentParsedResult.incLen(result.getLength());
                }
                if (bestLeftResult == null) {
                    buffer.backward(buffer.getPos() -  orgPos);
                    return Result.builder()
                            .pos(orgPos)
                            .errorMsg(ErrorUtil.error(buffer))
                            .build();
                }
                if (keepStopResult) {
                    buffer.backward(buffer.getPos() - orgPos);
                    buffer.forward(bestLeftResult.getLength() + bestStopResult.getLength());
                    return merge(bestLeftResult, bestStopResult);
                }
                buffer.backward(buffer.getPos() - orgPos);
                buffer.forward(bestLeftResult.getLength());
                return bestLeftResult;
            }
        };
    }

    private static Result merge(Result left , Result right) {
        Result result = Result.empty();
        result.addAll(left.getResult());
        result.addAll(right.getResult());
        result.incLen(left.getLength());
        result.incLen(right.getLength());
        return result;
    }


    /**
     * Map the result to another value.
     * @param mapper The mapper
     * @return A new parser that is composed of the mapper.
     */
    public Parser map(Function<List, ?> mapper) {
        return new Parser() {
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
        return new Parser() {
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
                        .pos(buffer.getPos())
                        .errorMsg("No suitable Parser to choose")
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
     * Perform parsing, but do not consume input
     * @return A new Parser
     */
    public Parser lookAhead() {
        return new Parser() {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = Parser.this.runParser(buffer);
                if (result.isError()) {
                    return result;
                }
                buffer.backward(result.length);
                return Result.empty();
            }
        };
    }

    /**
     * Do nothing but return success
     * @return A empty Parser
     */
    public static Parser empty() {
        return new Parser() {
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
        return new Parser() {
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
        return new Parser() {
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
            parser = parser.chain(p);
        }
        return parser;
    }
}
