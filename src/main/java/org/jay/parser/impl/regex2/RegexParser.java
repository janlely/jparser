package org.jay.parser.impl.regex2;

import org.apache.commons.lang3.StringUtils;
import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.F;
import org.jay.parser.util.Mapper;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

public class RegexParser {

    private AtomicInteger groupId;
    private Map<Integer, String> groupResult;

    private Parser compiledParser;

    public RegexParser() {
        this.groupId = new AtomicInteger(0);
        this.groupResult = new HashMap<>();
    }

    /**
     * "x*"
     * @return
     */
    public Parser many() {
        return validToken()
                .connect(TextParsers.one('*').ignore())
                .map(s -> toRepeat(Token.RepeatToken.builder().type(RepeatType.MANY).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x+"
     * @return
     */
    public Parser some() {
        return validToken()
                .connect(TextParsers.one('+').ignore())
                .map(s -> toRepeat(Token.RepeatToken.builder().type(RepeatType.SOME).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x{m,n}"
     * @return
     */
    public Parser range() {
        Parser rangeParser = TextParsers.one('{').ignore()
                .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .connect(() -> TextParsers.one(',').ignore())
                .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .connect(() -> TextParsers.one('}').ignore());
        return validToken().connect(rangeParser)
                .map(s -> toRepeat(Token.RepeatToken.builder().type(RepeatType.RANGE)
                                .value(new int[] {(int) s.get(1), (int) s.get(2)}).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x{n}"
     * @return
     */
    public Parser repeat() {
        Parser rangeParser = TextParsers.one('{').ignore()
                .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .connect(() -> TextParsers.one('}').ignore());
        return validToken().connect(rangeParser)
                .map(s -> toRepeat(Token.RepeatToken.builder().type(RepeatType.REPEAT)
                                .value(s.get(1)).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x?"
     * @return
     */
    public Parser optional() {
        return validToken().connect(TextParsers.one('?').ignore())
                .map(s -> toRepeat(Token.RepeatToken.builder().type(RepeatType.OPTIONAL).build(),
                        (RParser) s.get(0)));
    }

    public Parser start() {
        return TextParsers.one('^').killer().ignore();
    }

    public static Parser end() {
        return TextParsers.one('$')
                .map(Mapper.replace(TextParsers.eof()))
                .optional();
    }

    public Optional<String> match(String src) {
        Result result = this.compiledParser.runParser(Buffer.builder().data(src.getBytes()).build());
        if (result.isError()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    public Parser groupParser() {
        int groupId = this.groupId.getAndIncrement();
        return Parser.choose(
                () -> many(),
                () -> some(),
                () -> range(),
                () -> repeat(),
                () -> optional(),
                () -> validToken()
        ).many().map(s -> GroupResult.builder().groupId(groupId).value(s).build());
    }

    public void compile(String regex) {
        List<Parser> parsers = start().optional()
                .connect(() -> groupParser())
                .connect(() -> end().optional()).runParser(Buffer.builder().data(regex.getBytes()).build())
                .getResult();
        this.compiledParser = Parser.btConnect(true, parsers);
    }

    public Parser validToken() {
        return Parser.choose(
                () -> escape(),
                () -> TextParsers.one('.').map(Mapper.replace(RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(F.not(Character::isISOControl)))
                        .build())),
//                TextParsers.one('.').map(Mapper.replace(Token.builder().type(TokenType.VALID_CHAR)
//                        .value(Token.CharToken.builder().type(CharType.DOT).predicate(F.not(Character::isISOControl)).build()).build())),
                () -> TextParsers.one('[').ignore()
                        .connect(() -> select())
                        .connect(() -> TextParsers.one(']').ignore())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.PARSER)
                                .parser(TextParsers.satisfy((Predicate<Character>) s.get(0))).build()),
//                        .map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder()
//                                .type(CharType.SELECT)
//                                .predicate((Predicate<Character>) s.get(0))
//                                .build()).build()),
                () -> TextParsers.one('(').ignore()
                        .connect(() -> groupParser())
                        .connect(() ->TextParsers.one(')').ignore())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.GROUP)
                                .parser(RegexParser.btConnect(true,
                                        GroupResult.class.cast(s.get(0)).getValue(),
                                        this.groupResult,
                                        GroupResult.class.cast(s.get(0)).getGroupId()))
                                .build()),
//                        .map(s -> Token.builder().type(TokenType.GROUP).value(s.get(0)).build()),
                () -> TextParsers.one('\\').ignore()
                        .connect(() -> NumberParsers.anyIntStr())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.QUOTE)
                                .quoteId((int) s.get(0))
                                .build()),
//                        .map(s -> Token.builder().type(TokenType.QUOTE).value(s.get(0)).build()),
//                TextParsers.satisfy(validChar()).map(s -> Token.builder().type(TokenType.VALID_CHAR)
//                        .value(Token.CharToken.builder()
//                                .type(CharType.CHAR)
//                                .predicate(ch -> ch == s.get(0))
//                                .build()).build())
                () -> TextParsers.satisfy(validChar()).map(s -> RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(ch -> ch == s.get(0)))
                        .build())
        );

    }

    public static Parser btConnect(boolean greedy, List<RParser> parsers, Map<Integer, String> groupResult, int groupId) {
        if (parsers == null || parsers.isEmpty()) {
            return Parser.empty();
        }
        return new Parser("RegexParser.btConnect", new ArrayDeque<>()) {
            @Override
            public Result parse(IBuffer buffer) {
                Result result = null;
                RParser headParser = parsers.get(0);
                if (headParser.getType() == RParser.ParserType.QUOTE) {
                    if (!groupResult.containsKey(headParser.getQuoteId())) {
                        throw new InvalidRegexException("invalid group: " + headParser.getQuoteId());
                    }
                    Parser p = headParser.getFunc() == null
                            ? TextParsers.string(groupResult.get(headParser.getQuoteId()))
                            : headParser.getFunc().apply(TextParsers.string(groupResult.get(headParser.getQuoteId())));
                    return p.connect(() -> RegexParser.btConnect(greedy, parsers.subList(1, parsers.size()), groupResult, groupId))
                            .runParser(buffer);
                }
                for(int i = 0; i <= buffer.remaining(); i++) {
                    IBuffer[] tmp = buffer.splitAt(i);
                    IBuffer left = tmp[0];
                    IBuffer right = tmp[1];
                    Result headResult = headParser.getParser().runParser(left);
                    if (headResult.isError()) {
                        continue;
                    }
                    int gid = groupId;
                    if (headParser.getType() == RParser.ParserType.GROUP) {
                        gid++;
                        groupResult.put(groupId+1, StringUtils.join(headResult.getResult(), ""));
                    }
                    Result tailResult = RegexParser.btConnect(
                            greedy,
                            parsers.subList( 1, parsers.size()),
                            groupResult,
                            gid).runParser(right);
                    if (tailResult.isError()) {
                        continue;
                    }
                    Result curRes = Result.empty();
                    curRes.incLen(headResult.getLength());
                    curRes.addAll(headResult.getResult());
                    curRes.incLen(tailResult.getLength());
                    curRes.addAll(tailResult.getResult());
                    if (!greedy) {
                        return curRes;
                    }
                    if (result == null) {
                        result = curRes;
                    }else {
                        if (result.getLength() < curRes.getLength()) {
                            result = curRes;
                        }
                    }
                }
                return result == null ? Result.broken() : result;
            }
        };
    }

    public Parser escape() {
        return Parser.choose(
//                TextParsers.string("\\s").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.WHITE.getPredicate()).build()).build()),
//                TextParsers.string("\\S").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.NON_WHITE.getPredicate()).build()).build()),
//                TextParsers.string("\\d").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.DIGIT.getPredicate()).build()).build()),
//                TextParsers.string("\\D").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.NON_DIGIT.getPredicate()).build()).build()),
//                TextParsers.string("\\w").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.WORD.getPredicate()).build()).build()),
//                TextParsers.string("\\W").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.NON_WORD.getPredicate()).build()).build()),
//                TextParsers.string("\\.").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.DOT.getPredicate()).build()).build()),
//                TextParsers.string("\\(").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.LEFT_BRACKET.getPredicate()).build()).build()),
//                TextParsers.string("\\)").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.RIGHT_BRACKET.getPredicate()).build()).build()),
//                TextParsers.string("\\[").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.LEFT_SQUARE_BRACKET.getPredicate()).build()).build()),
//                TextParsers.string("\\]").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.RIGHT_SQUARE_BRACKET.getPredicate()).build()).build()),
//                TextParsers.string("\\\\").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.BACKSLASH.getPredicate()).build()).build()),
//                TextParsers.string("\\+").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.PLUS.getPredicate()).build()).build()),
//                TextParsers.string("\\*").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.STAR.getPredicate()).build()).build()),
//                TextParsers.string("\\?").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.QUESTION_MARK.getPredicate()).build()).build())
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

    public static Parser select() {
        Parser range = TextParsers.satisfy(Character::isLetterOrDigit)
                .connect(() -> TextParsers.one('-').ignore())
                .connect(() -> TextParsers.satisfy(Character::isLetterOrDigit))
                .map(s -> {
                    return (Predicate<Character>) character -> {
                        return character >= (Character) s.get(0) && (Character) s.get(1) >= character;
                    };
                });
        Function<List, ?> mapper = s -> (Predicate<Character>) character -> {
            Optional<Predicate> p = s.stream().reduce((a, b) -> (Predicate<Character>) x -> {
                return Predicate.class.cast(a).test(x) || Predicate.class.cast(b).test(x);
            });
            return p.get().test(character);
        };
        return TextParsers.one('^').optional()
                .connect(() -> Parser.choose(
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

    private RParser toRepeat(Token.RepeatToken token, RParser base) {
        switch (token.getType()) {
            case MANY:
                return base.apply(p -> p.many());
            case SOME:
                return base.apply(p -> p.some());
            case RANGE:
                int[] range = token.getValue();
                return base.apply(p -> p.range(range[0], range[1]));
            case REPEAT:
                return base.apply(p -> p.repeat(token.getValue()));
            case OPTIONAL:
                return base.apply(p -> p.optional());
        }
        throw new RuntimeException("unrecognized RepeatToken, type: " + token.getType().name());
    }

    private List subList(List src, int start, int end) {
        return null;
    }

}
