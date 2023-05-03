package org.jay.parser.impl.regex;

import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.ChooseParser;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.F;
import org.jay.parser.util.Mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class RegexParser {


    public static Optional<String> match(Parser parser, String src) {
        Result result = run(parser, src);
        if (result.isError()) {
            return Optional.empty();
        }
        return Optional.ofNullable(toStr().apply(result.getResult()));
    }

    public static Result run(Parser parser, String src) {
        if (parser.isKiller()) {
            return parser.runParser(Buffer.builder().data(src.getBytes()).build());
        }
        return Parser.scan(TextParsers.skip(1), parser)
                .runParser(Buffer.builder().data(src.getBytes()).build());
    }

    public static Parser compile(String regex) {
        return start().optional()
                .connect(() -> tokenParser())
                .connect(() -> end().optional())
                .map(tokensMapper()).runParser(Buffer.builder().data(regex.getBytes()).build())
                .get(0);
    }
    public static Parser tokenParser() {
        return token().many();
    }

    public static Parser start() {
        return TextParsers.one('^')
                .killer()
                .map(Mapper.replace(Token.builder().type(TokenType.START).build()))
                .optional();
    }

    public static Parser end() {
        return TextParsers.one('$')
                .map(Mapper.replace(Token.builder().type(TokenType.END).build()))
                .optional();
    }

    public static Parser token() {
        return ChooseParser.choose(
                escape(),
                TextParsers.one('.').map(Mapper.replace(Token.builder().type(TokenType.VALID_CHAR)
                        .value(Token.CharToken.builder().type(CharType.DOT).predicate(F.not(Character::isISOControl)).build()).build())),
                TextParsers.one('+').map(Mapper.replace(Token.builder().type(TokenType.REPEAT).value(RepeatType.SOME).build())),
                TextParsers.one('*').map(Mapper.replace(Token.builder().type(TokenType.REPEAT).value(RepeatType.MANY).build())),
                TextParsers.one('{').ignore()
                        .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                        .connect(() -> TextParsers.one(',').ignore())
                        .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                        .connect(() -> TextParsers.one('}').ignore())
                        .map(s -> Token.builder().type(TokenType.REPEAT)
                                .value(Token.RepeatToken.builder()
                                        .type(RepeatType.RANGE).value(new int[] {(int) s.get(0), (int) s.get(1)})
                                        .build())
                                .build()),
                TextParsers.one('{').ignore()
                        .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                        .connect(() -> TextParsers.one('}').ignore())
                        .map(s -> Token.builder().type(TokenType.REPEAT).value(Token.RepeatToken.builder()
                                        .value(s.get(0))
                                        .type(RepeatType.REPEAT)
                                        .build())
                                .build()),
                TextParsers.one('?').map(Mapper.replace(Token.builder()
                        .type(TokenType.REPEAT)
                        .value(Token.RepeatToken.builder()
                                .type(RepeatType.OPTIONAL)
                                .build())
                        .build())),
                TextParsers.one('[').ignore()
                        .connect(() -> select())
                        .connect(() -> TextParsers.one(']').ignore())
                        .map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder()
                                        .type(CharType.SELECT)
                                        .predicate((Predicate<Character>) s.get(0))
                                .build()).build()),
                TextParsers.one('(').ignore()
                        .connect(() -> tokenParser())
                        .connect(() ->TextParsers.one(')').ignore())
                        .map(s -> Token.builder().type(TokenType.GROUP).value(s.get(0)).build()),
                TextParsers.one('\\').ignore()
                        .connect(() -> NumberParsers.anyIntStr())
                        .map(s -> Token.builder().type(TokenType.QUOTE).value(s.get(0)).build()),
                TextParsers.one('|').map(Mapper.replace(Token.builder().type(TokenType.OR))),
                TextParsers.satisfy(validChar()).map(s -> Token.builder().type(TokenType.VALID_CHAR)
                        .value(Token.CharToken.builder()
                                .type(CharType.CHAR)
                                .predicate(ch -> ch == s.get(0))
                                .build()).build())
        );
    }

    private static Predicate<Character> validChar() {
        return ch -> ch != '^' && ch != '$' && !Character.isISOControl(ch);
    }

    public static Parser escape() {
        return ChooseParser.choose(
                TextParsers.string("\\s").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.WHITE.getPredicate()).build()).build()),
                TextParsers.string("\\S").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.NON_WHITE.getPredicate()).build()).build()),
                TextParsers.string("\\d").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.DIGIT.getPredicate()).build()).build()),
                TextParsers.string("\\D").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.NON_DIGIT.getPredicate()).build()).build()),
                TextParsers.string("\\w").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.WORD.getPredicate()).build()).build()),
                TextParsers.string("\\W").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.NON_WORD.getPredicate()).build()).build()),
                TextParsers.string("\\.").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.DOT.getPredicate()).build()).build()),
                TextParsers.string("\\(").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.LEFT_BRACKET.getPredicate()).build()).build()),
                TextParsers.string("\\)").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.RIGHT_BRACKET.getPredicate()).build()).build()),
                TextParsers.string("\\[").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.LEFT_SQUARE_BRACKET.getPredicate()).build()).build()),
                TextParsers.string("\\]").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.RIGHT_SQUARE_BRACKET.getPredicate()).build()).build()),
                TextParsers.string("\\\\").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.BACKSLASH.getPredicate()).build()).build()),
                TextParsers.string("\\+").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.PLUS.getPredicate()).build()).build()),
                TextParsers.string("\\*").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.STAR.getPredicate()).build()).build()),
//                TextParsers.string("\\^").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.UP_ARROW.getPredicate()).build()).build()),
//                TextParsers.string("\\$").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.DOLLAR.getPredicate()).build()).build()),
                TextParsers.string("\\?").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.QUESTION_MARK.getPredicate()).build()).build())
        );
    }

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
                .connect(() -> ChooseParser.choose(
                        range,
                        TextParsers.satisfy(Character::isLetterOrDigit).map(s -> (Predicate<Character>) character -> character == s.get(0))
                ).some().map(mapper))
                .map(s -> {
                    if (s.size() == 1) {
                        return Predicate.class.cast(s.get(0));
                    }
                    return (Predicate<Character>) character -> !Predicate.class.cast(s.get(1)).test(character);
                });
    }

    /**
     * map [Token] to a Parser
     * @return
     */
    public static Function<List, Parser> tokensMapper() {
        return tokens -> new ResultParser(0, tokens, new HashMap<>()).generateParse();
    }

    public static Function<List, String> toStr() {
        return values -> {
            StringBuilder result = new StringBuilder();
            for (Object value : values) {
                if (value instanceof Character) {
                    result.append(Character.class.cast(value));
                }
                if (value instanceof GroupResult) {
                    result.append(toStr().apply(GroupResult.class.cast(value).getValue()));
                }
            }
            return result.toString();
        };
    }

}
