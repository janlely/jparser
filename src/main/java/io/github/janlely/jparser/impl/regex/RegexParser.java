package io.github.janlely.jparser.impl.regex;

import org.apache.commons.lang3.StringUtils;
import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.comb.BacktraceParser;
import io.github.janlely.jparser.parsers.NumberParsers;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Buffer;
import io.github.janlely.jparser.util.F;
import io.github.janlely.jparser.util.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * the regex Parser
 */
public class RegexParser {

    /**
     * Auto-incremented group ID.
     */
    private AtomicInteger groupId;
    /**
     * group result cache
     */
    private Map<Integer, String> groupResult;
    /**
     * final group result
     */
    private Map<Integer, String> finalGroup;
    /**
     * compiled Parser
     */
    private Parser compiledParser;

    /**
     * constructor
     */
    public RegexParser() {
        this.groupId = new AtomicInteger(0);
        this.groupResult = new HashMap<>();
        this.finalGroup = new HashMap<>();
    }

    /**
     * "x*"
     * @return A new Parser
     */
    public Parser many() {
        return validToken()
                .chain(TextParsers.one('*').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.MANY).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x+"
     * @return A new Parser
     */
    public Parser some() {
        return validToken()
                .chain(TextParsers.one('+').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.SOME).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x{m,n}"
     * @return A new Parser
     */
    public Parser range() {
        Parser rangeParser = TextParsers.one('{').ignore()
                .chain(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .chain(() -> TextParsers.one(',').ignore())
                .chain(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .chain(() -> TextParsers.one('}').ignore());
        return validToken().chain(rangeParser)
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.RANGE)
                                .value(new int[] {(int) s.get(1), (int) s.get(2)}).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x{n}"
     * @return A new Parser
     */
    public Parser repeat() {
        Parser rangeParser = TextParsers.one('{').ignore()
                .chain(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .chain(() -> TextParsers.one('}').ignore());
        return validToken().chain(rangeParser)
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.REPEAT)
                                .value(s.get(1)).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x?"
     * @return A new Parser
     */
    public Parser optional() {
        return validToken().chain(TextParsers.one('?').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.OPTIONAL).build(),
                        (RParser) s.get(0)));
    }

    /**
     * the '^'
     * @return A new Parser
     */
    public Parser start() {
        return TextParsers.one('^').map(Mapper.replace(RParser.builder().type(RParser.ParserType.START).build()));
    }

    /**
     * the '$'
     * @return A new Parser
     */
    public static Parser end() {
        return TextParsers.one('$')
                .map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.eof()).build()))
                .optional();
    }

    /**
     * @return the regex Parser
     */
    public Parser parser() {
        return Parser.choose(
                () -> many(),
                () -> some(),
                () -> range(),
                () -> repeat(),
                () -> optional(),
                () -> validToken()
        ).many().map(s -> RParser.builder().parser(chainParsers(s))
                .type(RParser.ParserType.PARSER)
                .build());
    }

    /**
     * @param rParsers RParsers to chain
     * @return Chained Parser
     */
    private Parser chainParsers(List<RParser> rParsers) {
        List<Parser> parsers = rParsers.stream().map(rp -> {
            if (rp.getType() == RParser.ParserType.GROUP) {
                return new AopParser(rp.getParser(), () -> {
                    if (rp.getGroupId() == 0) {
                        rp.setGroupId(groupId.incrementAndGet());
                    }
                }, res -> {
                    if (res.isSuccess()) {
                        groupResult.put(rp.getGroupId(), res.get(0));
                    }
                });
            }
            if (rp.getType() == RParser.ParserType.QUOTE) {
                return Parser.empty().chain(() -> {
                    if (!groupResult.containsKey(rp.getQuoteId())) {
                        throw new InvalidRegexException("invalid group: " + rp.getQuoteId());
                    }
                    Parser base = TextParsers.string(groupResult.get(rp.getQuoteId()));
                    if (rp.getFunc() == null) {
                        return base;
                    }
                    return rp.getFunc().apply(base);
                });
            }
            return rp.getParser();
        }).collect(Collectors.toList());
        if (parsers.isEmpty()) {
            throw new RuntimeException("No RParser to chain");
        }
        if (parsers.size() == 1) {
            return parsers.get(0);
        }
        List<Supplier<Parser>> suppliers = new ArrayList<>();
        for(int i = 0; i < parsers.size(); i++) {
            int idx = i;
            suppliers.add(() -> parsers.get(idx).map(Mapper.toStr()));
        }

        BacktraceParser parser = new BacktraceParser(true, suppliers);
        return parser;
    }


    /**
     * clean properties
     */
    private void clean() {
        this.groupId.set(0);
        this.groupResult.clear();
        this.finalGroup.clear();
    }

    /**
     * @param src string to be matched
     * @return group result
     */
    public List<String> search(String src) {
        clean();
        Optional<String> result = match(src);
        if (result.isEmpty()) {
            return new ArrayList<>();
        }
        this.finalGroup.put(0, result.get());
        return this.finalGroup.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(en -> en.getValue()).collect(Collectors.toList());
    }

    public Optional<String> match(String src) {
        Result result = this.compiledParser.runParser(Buffer.builder().data(src.getBytes()).build());
        if (result.isSuccess()) {
            return Optional.of(result.get(0));
        }
        return Optional.empty();
    }

    /**
     * @param regex the regex
     */
    public void compile(String regex) {
        Parser parserParser = start().optional()
                .chain(() -> parser())
                .chain(() -> end().optional());
        List<RParser> parsers = parserParser.runParser(Buffer.builder().data(regex.getBytes()).build())
                .getResult();
        switch (parsers.size()) {
            case 1:
                //Excluding the head(^) and tail($), scanning is required
                BacktraceParser mainParser = (BacktraceParser) parsers.get(0).getParser();
                mainParser.onResultFound(() -> {
                    this.finalGroup.putAll(groupResult);
                });
                this.compiledParser = mainParser.map(Mapper.toStr()).scan(() -> {
                    this.groupResult.clear();
                    this.groupId.set(0);
                    return TextParsers.skip(1);
                });
                break;
            case 2:
                //including head(^), scanning is not required
                if (parsers.get(0).getType() == RParser.ParserType.START) {
                    mainParser = (BacktraceParser) parsers.get(1).getParser();
                    mainParser.onResultFound(() -> {
                        this.finalGroup.putAll(groupResult);
                    });
                    this.compiledParser = mainParser.map(Mapper.toStr());
                }else {
                //including tail($), followed by a eof() and scanning is required
                    mainParser = (BacktraceParser) parsers.get(0).getParser();
                    mainParser.onResultFound(() -> {
                        this.finalGroup.putAll(groupResult);
                    });
                    this.compiledParser = mainParser.map(Mapper.toStr()).scan(() -> {
                                this.groupResult.clear();
                                this.groupId.set(0);
                                return TextParsers.skip(1);
                            })
                            .chain(() -> TextParsers.eof());
                }
                break;
            case 3:
                //including the head(^) and tail($), followed by a eof() and scanning is required
                mainParser = (BacktraceParser) parsers.get(1).getParser();
                mainParser.onResultFound(() -> {
                    this.finalGroup.putAll(groupResult);
                });
                this.compiledParser = mainParser.map(Mapper.toStr()).chain(() -> TextParsers.eof());
                break;
        }
    }

    /**
     * @return token Parser
     */
    public Parser validToken() {
        return Parser.choose(
                () -> escape(),
                () -> TextParsers.one('.').map(Mapper.replace(RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(F.not(Character::isISOControl)))
                        .build())),
                () -> TextParsers.one('[').ignore()
                        .chain(() -> select())
                        .chain(() -> TextParsers.one(']').ignore())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.PARSER)
                                .parser(TextParsers.satisfy((Predicate<Character>) s.get(0))).build()),
                () -> TextParsers.one('\\').ignore()
                        .chain(() -> NumberParsers.anyIntStr())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.QUOTE)
                                .quoteId((int) s.get(0))
                                .build()),
                () -> TextParsers.satisfy(validChar()).map(s -> RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(ch -> ch == s.get(0)))
                        .build()),
                () -> TextParsers.one('(').ignore()
                        .chain(() -> parser())
                        .chain(() ->TextParsers.one(')').ignore())
                        .map(s -> {
                            RParser rp = RParser.class.cast(s.get(0));
                            rp.setType(RParser.ParserType.GROUP);
                            return rp;
                        })
        );

    }

    /**
     * @return escape Parser
     */
    public Parser escape() {
        return Parser.choose(
                () -> TextParsers.string("\\s").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.WHITE.getPredicate())).build())),
                () -> TextParsers.string("\\S").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.NON_WHITE.getPredicate())).build())),
                () -> TextParsers.string("\\d").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.DIGIT.getPredicate())).build())),
                () -> TextParsers.string("\\D").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.NON_DIGIT.getPredicate())).build())),
                () -> TextParsers.string("\\w").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.WORD.getPredicate())).build())),
                () -> TextParsers.string("\\W").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.NON_WORD.getPredicate())).build())),
                () -> TextParsers.string("\\.").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.DOT.getPredicate())).build())),
                () -> TextParsers.string("\\(").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.LEFT_BRACKET.getPredicate())).build())),
                () -> TextParsers.string("\\)").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.RIGHT_BRACKET.getPredicate())).build())),
                () -> TextParsers.string("\\[").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.LEFT_SQUARE_BRACKET.getPredicate())).build())),
                () -> TextParsers.string("\\]").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.RIGHT_SQUARE_BRACKET.getPredicate())).build())),
                () -> TextParsers.string("\\\\").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.BACKSLASH.getPredicate())).build())),
                () -> TextParsers.string("\\+").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.PLUS.getPredicate())).build())),
                () -> TextParsers.string("\\*").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.STAR.getPredicate())).build())),
                () -> TextParsers.string("\\?").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.QUESTION_MARK.getPredicate())).build()))
        );
    }

    /**
     * [selectors]
     * @return select Parser
     */
    public static Parser select() {
        Parser range = TextParsers.satisfy(Character::isLetterOrDigit)
                .chain(() -> TextParsers.one('-').ignore())
                .chain(() -> TextParsers.satisfy(Character::isLetterOrDigit))
                .map(s -> (Predicate<Character>) character -> character >= (Character) s.get(0) && (Character) s.get(1) >= character);
        Function<List, ?> mapper = s -> (Predicate<Character>) character -> {
            Optional<Predicate> p = s.stream().reduce((a, b) -> (Predicate<Character>) x -> Predicate.class.cast(a).test(x) || Predicate.class.cast(b).test(x));
            return p.get().test(character);
        };
        return TextParsers.one('^').optional()
                .chain(() -> Parser.choose(
                        () -> range,
                        () -> TextParsers.string("\\[").map(Mapper.replace('[')),
                        () -> TextParsers.string("\\]").map(Mapper.replace(']')),
                        () -> TextParsers.satisfy(F.noneOf(Character::isISOControl, ch -> ch == ']'))
                                .map(s -> (Predicate<Character>) character -> character == s.get(0))
                ).some().map(mapper))
                .map(s -> {
                    if (s.size() == 1) {
                        return Predicate.class.cast(s.get(0));
                    }
                    return (Predicate<Character>) character -> !Predicate.class.cast(s.get(1)).test(character);
                });
    }


    /**
     * @return predicate of a valid character
     */
    public Predicate<Character> validChar() {
        return ch -> !StringUtils.contains("^$+*.?{}()", ch);
    }

    /**
     * validToken + repeat
     * @param token The RepeatToken
     * @param base base Parser
     * @return A new Parser
     */
    private RParser toRepeat(RepeatToken token, RParser base) {
        switch (token.getType()) {
            case MANY:
                return base.apply(p -> p.many().map(Mapper.toStr()));
            case SOME:
                return base.apply(p -> p.some().map(Mapper.toStr()));
            case RANGE:
                int[] range = token.getValue();
                return base.apply(p -> p.range(range[0], range[1]).map(Mapper.toStr()));
            case REPEAT:
                return base.apply(p -> p.repeat(token.getValue()).map(Mapper.toStr()));
            case OPTIONAL:
                return base.apply(p -> p.optional());
        }
        throw new RuntimeException("unrecognized RepeatToken, type: " + token.getType().name());
    }

}
