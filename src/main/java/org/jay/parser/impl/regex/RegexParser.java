package org.jay.parser.impl.regex;

import org.apache.commons.lang3.StringUtils;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.comb.BacktraceParser;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.F;
import org.jay.parser.util.Mapper;

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

public class RegexParser {

    private AtomicInteger groupId;
    private Map<Integer, String> groupResult;
    private Map<Integer, String> finalGroup;
    private Parser compiledParser;

    public RegexParser() {
        this.groupId = new AtomicInteger(0);
        this.groupResult = new HashMap<>();
        this.finalGroup = new HashMap<>();
    }

    /**
     * "x*"
     * @return
     */
    public Parser many() {
        return validToken()
                .concat(TextParsers.one('*').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.MANY).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x+"
     * @return
     */
    public Parser some() {
        return validToken()
                .concat(TextParsers.one('+').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.SOME).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x{m,n}"
     * @return
     */
    public Parser range() {
        Parser rangeParser = TextParsers.one('{').ignore()
                .concat(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .concat(() -> TextParsers.one(',').ignore())
                .concat(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .concat(() -> TextParsers.one('}').ignore());
        return validToken().concat(rangeParser)
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.RANGE)
                                .value(new int[] {(int) s.get(1), (int) s.get(2)}).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x{n}"
     * @return
     */
    public Parser repeat() {
        Parser rangeParser = TextParsers.one('{').ignore()
                .concat(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .concat(() -> TextParsers.one('}').ignore());
        return validToken().concat(rangeParser)
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.REPEAT)
                                .value(s.get(1)).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x?"
     * @return
     */
    public Parser optional() {
        return validToken().concat(TextParsers.one('?').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.OPTIONAL).build(),
                        (RParser) s.get(0)));
    }

    public Parser start() {
        return TextParsers.one('^').map(Mapper.replace(RParser.builder().type(RParser.ParserType.START).build()));
    }

    public static Parser end() {
        return TextParsers.one('$')
                .map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.eof()).build()))
                .optional();
    }

    public Parser parser() {
        return Parser.choose(
                () -> many(),
                () -> some(),
                () -> range(),
                () -> repeat(),
                () -> optional(),
                () -> validToken()
        ).many().map(s -> {
            return RParser.builder().parser(chain(s).map(Mapper.toStr()))
                    .type(RParser.ParserType.PARSER)
                    .build();
        });
    }

    private Parser chain(List<RParser> rParsers) {
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
                return Parser.empty().concat(() -> {
                    if (!groupResult.containsKey(rp.getQuoteId())) {
                        throw new InvalidRegexException("invalid group: " + rp.getQuoteId());
                    }
                    return TextParsers.string(groupResult.get(rp.getQuoteId()));
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
        List<Supplier<Parser>> tail = new ArrayList<>();
        for(int i = 0; i < parsers.size(); i++) {
            int idx = i;
            tail.add(() -> parsers.get(idx));
        }

        BacktraceParser parser = new BacktraceParser(true, tail);
        parser.setRunnable(() -> {
            this.finalGroup.putAll(groupResult);
        });
        return parser;
    }


    private void clean() {
        this.groupId.set(0);
        this.groupResult.clear();
        this.finalGroup.clear();
    }
    public List<String> search(String src) {
        clean();
        Optional<String> result = match(src);
        if (result.isPresent()) {
            this.groupResult.put(0, result.get());
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
    public void compile(String regex) {
        Parser parserParser = start().optional()
                .concat(() -> parser())
                .concat(() -> end().optional());
        List<RParser> parsers = parserParser.runParser(Buffer.builder().data(regex.getBytes()).build())
                .getResult();
        switch (parsers.size()) {
            case 1:
                //Excluding the head(^) and tail($), scanning is required
                this.compiledParser = parsers.get(0).getParser().scan(() -> {
                    this.groupResult.clear();
                    this.groupId.set(0);
                    return TextParsers.skip(1);
                });
                break;
            case 2:
                //including head(^), scanning is not required
                if (parsers.get(0).getType() == RParser.ParserType.START) {
                    this.compiledParser = parsers.get(1).getParser();
                }else {
                //including tail($), followed by a eof() and scanning is required
                    this.compiledParser = parsers.get(0).getParser()
                            .scan(() -> {
                                this.groupResult.clear();
                                this.groupId.set(0);
                                return TextParsers.skip(1);
                            })
                            .concat(() -> TextParsers.eof());
                }
                break;
            case 3:
                //including the head(^) and tail($), followed by a eof() and scanning is required
                this.compiledParser = parsers.get(1).getParser().concat(() -> TextParsers.eof());
                break;
        }
    }

    /**
     * escape, [], \\, (), char
     * @return
     */
    public Parser validToken() {
        return Parser.choose(
                () -> escape(),
                () -> TextParsers.one('.').map(Mapper.replace(RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(F.not(Character::isISOControl)))
                        .build())),
                () -> TextParsers.one('[').ignore()
                        .concat(() -> select())
                        .concat(() -> TextParsers.one(']').ignore())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.PARSER)
                                .parser(TextParsers.satisfy((Predicate<Character>) s.get(0))).build()),
                () -> TextParsers.one('\\').ignore()
                        .concat(() -> NumberParsers.anyIntStr())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.QUOTE)
                                .quoteId((int) s.get(0))
                                .build()),
                () -> TextParsers.satisfy(validChar()).map(s -> RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(ch -> ch == s.get(0)))
                        .build()),
                () -> TextParsers.one('(').ignore()
                        .concat(() -> parser())
                        .concat(() ->TextParsers.one(')').ignore())
                        .map(s -> {
                            RParser rp = RParser.class.cast(s.get(0));
                            rp.setType(RParser.ParserType.GROUP);
                            return rp;
                        })
        );

    }

    /**
     * \s \S \w \W ....
     * @return
     */
    public Parser escape() {
        return Parser.choose(
                TextParsers.string("\\s").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.WHITE.getPredicate())).build())),
                TextParsers.string("\\S").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.NON_WHITE.getPredicate())).build())),
                TextParsers.string("\\d").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.DIGIT.getPredicate())).build())),
                TextParsers.string("\\D").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.NON_DIGIT.getPredicate())).build())),
                TextParsers.string("\\w").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.WORD.getPredicate())).build())),
                TextParsers.string("\\W").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.NON_WORD.getPredicate())).build())),
                TextParsers.string("\\.").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.DOT.getPredicate())).build())),
                TextParsers.string("\\(").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.LEFT_BRACKET.getPredicate())).build())),
                TextParsers.string("\\)").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.RIGHT_BRACKET.getPredicate())).build())),
                TextParsers.string("\\[").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.LEFT_SQUARE_BRACKET.getPredicate())).build())),
                TextParsers.string("\\]").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.RIGHT_SQUARE_BRACKET.getPredicate())).build())),
                TextParsers.string("\\\\").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.BACKSLASH.getPredicate())).build())),
                TextParsers.string("\\+").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.PLUS.getPredicate())).build())),
                TextParsers.string("\\*").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.STAR.getPredicate())).build())),
                TextParsers.string("\\?").map(Mapper.replace(RParser.builder().type(RParser.ParserType.PARSER).parser(TextParsers.satisfy(EscapeToken.QUESTION_MARK.getPredicate())).build()))
        );
    }

    /**
     * [selectors]
     * @return
     */
    public static Parser select() {
        Parser range = TextParsers.satisfy(Character::isLetterOrDigit)
                .concat(() -> TextParsers.one('-').ignore())
                .concat(() -> TextParsers.satisfy(Character::isLetterOrDigit))
                .map(s -> (Predicate<Character>) character -> character >= (Character) s.get(0) && (Character) s.get(1) >= character);
        Function<List, ?> mapper = s -> (Predicate<Character>) character -> {
            Optional<Predicate> p = s.stream().reduce((a, b) -> (Predicate<Character>) x -> Predicate.class.cast(a).test(x) || Predicate.class.cast(b).test(x));
            return p.get().test(character);
        };
        return TextParsers.one('^').optional()
                .concat(() -> Parser.choose(
                        range,
                        TextParsers.string("\\[").map(Mapper.replace('[')),
                        TextParsers.string("\\]").map(Mapper.replace(']')),
                        TextParsers.satisfy(F.noneOf(Character::isISOControl, ch -> ch == ']'))
                                .map(s -> (Predicate<Character>) character -> character == s.get(0))
                ).some().map(mapper))
                .map(s -> {
                    if (s.size() == 1) {
                        return Predicate.class.cast(s.get(0));
                    }
                    return (Predicate<Character>) character -> !Predicate.class.cast(s.get(1)).test(character);
                });
    }


    public Predicate<Character> validChar() {
        return ch -> !StringUtils.contains("^$+*.?{}()", ch);
    }

    /**
     * validToken + repeat
     * @param token
     * @param base
     * @return
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
