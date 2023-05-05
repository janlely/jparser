package org.jay.parser.impl.regex;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.brick.common.types.Pair;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.MANY).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x+"
     * @return
     */
    public Parser some() {
        return validToken()
                .connect(TextParsers.one('+').ignore())
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.SOME).build(),
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
                .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                .connect(() -> TextParsers.one('}').ignore());
        return validToken().connect(rangeParser)
                .map(s -> toRepeat(RepeatToken.builder().type(RepeatType.REPEAT)
                                .value(s.get(1)).build(),
                        (RParser) s.get(0)));
    }

    /**
     * "x?"
     * @return
     */
    public Parser optional() {
        return validToken().connect(TextParsers.one('?').ignore())
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
            return RParser.builder().parser(RegexParser.btConnect(true, groupResult, this.groupId, s).map(Mapper.toStr()))
                    .type(RParser.ParserType.PARSER)
                    .build();
        });
    }

    public List<String> search(String src) {
        this.groupId.set(0);
        this.groupResult.clear();
        Optional<String> result = match(src);
        if (result.isPresent()) {
            this.groupResult.put(0, result.get());
        }
        return this.groupResult.entrySet().stream()
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
                .connect(() -> parser())
                .connect(() -> end().optional());
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
                            .connect(() -> TextParsers.eof());
                }
                break;
            case 3:
                //including the head(^) and tail($), followed by a eof() and scanning is required
                this.compiledParser = parsers.get(1).getParser().connect(() -> TextParsers.eof());
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
                        .connect(() -> select())
                        .connect(() -> TextParsers.one(']').ignore())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.PARSER)
                                .parser(TextParsers.satisfy((Predicate<Character>) s.get(0))).build()),
                () -> TextParsers.one('\\').ignore()
                        .connect(() -> NumberParsers.anyIntStr())
                        .map(s -> RParser.builder()
                                .type(RParser.ParserType.QUOTE)
                                .quoteId((int) s.get(0))
                                .build()),
                () -> TextParsers.satisfy(validChar()).map(s -> RParser.builder()
                        .type(RParser.ParserType.PARSER)
                        .parser(TextParsers.satisfy(ch -> ch == s.get(0)))
                        .build()),
                () -> TextParsers.one('(').ignore()
                        .connect(() -> parser())
                        .connect(() ->TextParsers.one(')').ignore())
                        .map(s -> {
                            RParser rp = RParser.class.cast(s.get(0));
                            rp.setType(RParser.ParserType.GROUP);
                            return rp;
                        })
        );

    }

    /**
     * backtracing enabled connect
     * @param greedy
     * @param groupResult
     * @param groupId
     * @param parsers
     * @return
     */
    public static Parser btConnect(boolean greedy, Map<Integer,String> groupResult, AtomicInteger groupId, List<RParser> parsers) {
        if (parsers == null || parsers.isEmpty()) {
            return Parser.empty();
        }
        List<String> labels = parsers.stream().flatMap(r -> {
            if (r.getType() == RParser.ParserType.QUOTE) {
                return Stream.of("QUOTE + " + r.getQuoteId());
            }
            return r.getParser().getQueue().stream();
        }).collect(Collectors.toList());
        return new Parser("RegexParser.btConnect", new ArrayDeque<>(labels)) {
            @Override
            public Result parse(IBuffer buffer) {
                RParser headParser = parsers.get(0);
                //处理引用
                if (headParser.getType() == RParser.ParserType.QUOTE) {
                    if (!groupResult.containsKey(headParser.getQuoteId()))  {
                        throw new InvalidRegexException("invalid group: " + headParser.getQuoteId());
                    }
                    Parser p = headParser.getFunc() == null
                            ? TextParsers.string(groupResult.get(headParser.getQuoteId()))
                            : headParser.getFunc().apply(TextParsers.string(groupResult.get(headParser.getQuoteId())));
                    return p.connect(() -> RegexParser.btConnect(greedy, groupResult, groupId, parsers.subList(1, parsers.size())))
                            .runParser(buffer);
                }
                //处理非引用
                if (headParser.getType() == RParser.ParserType.GROUP) {
                    headParser.setGroupId(groupId.incrementAndGet());
                }
                LoopObject lp = LoopObject.builder()
                        .greedy(greedy)
                        .current(false)
                        .succeeded(false)
                        .idx(0)
                        .headParser(headParser)
                        .htResult(new Pair<>(Result.broken(), Result.broken()))
                        .build();
                Parser tailParser = RegexParser.btConnect(greedy, groupResult, groupId, parsers.subList(1, parsers.size()));
                while(!lp.end(buffer)) {
                    IBuffer[] tmp = buffer.splitAt(lp.getIdx());
                    IBuffer left = tmp[0];
                    IBuffer right = tmp[1];
                    Result headResult = headParser.getParser().runParser(left);
                    if (headResult.isError()) {
                        lp.current = false;
                        lp.idx++;
                        continue;
                    }
                    lp.succeeded = true;
                    lp.current = true;
                    //如果是group解析器就缓存group的结果
                    if (lp.getHeadParser().getType() == RParser.ParserType.GROUP) {
                        groupResult.put(headParser.getGroupId(), StringUtils.join(headResult.getResult(), ""));
                    }
                    Result tailResult = tailParser.runParser(right);
                    if (tailResult.isError()) {
                        lp.idx++;
                        continue;
                    }
                    if (!lp.isSuccess()) {
                        lp.setHtResult(new Pair<>(headResult, tailResult));
                    }
                    //当前结果不是最长的，取之前更长的
                    if (lp.isSuccess() && lp.getLen() < headResult.getLength() + tailResult.getLength()) {
                        lp.setHtResult(new Pair<>(headResult, tailResult));
                    }
                    lp.idx++;
                }
                if (!lp.isSuccess()) {
                    return Result.broken();
                }
                Result result = Result.empty();
                result.addAll(Pair.getLeft(lp.getHtResult()).getResult());
                result.addAll(Pair.getRight(lp.getHtResult()).getResult());
                result.incLen(Pair.getLeft(lp.getHtResult()).getLength());
                result.incLen(Pair.getRight(lp.getHtResult()).getLength());
                buffer.forward(result.getLength());
                return result;
            }
        };
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
                .connect(() -> TextParsers.one('-').ignore())
                .connect(() -> TextParsers.satisfy(Character::isLetterOrDigit))
                .map(s -> (Predicate<Character>) character -> character >= (Character) s.get(0) && (Character) s.get(1) >= character);
        Function<List, ?> mapper = s -> (Predicate<Character>) character -> {
            Optional<Predicate> p = s.stream().reduce((a, b) -> (Predicate<Character>) x -> Predicate.class.cast(a).test(x) || Predicate.class.cast(b).test(x));
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

    @Data
    @Builder
    public static class LoopObject {
        //use by splitAt
        private int idx;
        //Have succeeded before
        private boolean succeeded;
        //Is it currently a success?
        private boolean current;
        //head result and tail result
        private Pair<Result, Result> htResult;
        private RParser headParser;
        private boolean greedy;

        public boolean isSuccess() {
            return Pair.getLeft(htResult).isSuccess() && Pair.getRight(htResult).isSuccess();
        }

        public int getLen() {
            return Pair.getLeft(htResult).getLength() + Pair.getRight(htResult).getLength();
        }

        public boolean end(IBuffer buffer) {
            return this.idx > buffer.remaining() //buffer还可以切分
                    || (!this.greedy && isSuccess()) //非贪婪模式，并且已经匹配到了
                    || (this.headParser.getType() != RParser.ParserType.GROUP //这是一个group解析器
                    && this.greedy && isSucceeded() && !isCurrent()); //贪婪模式，之前成功过，当前是失败的，以后肯定不会再成功
        }
    }

}
