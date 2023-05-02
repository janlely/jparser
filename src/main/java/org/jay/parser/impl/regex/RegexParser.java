package org.jay.parser.impl.regex;

import org.jay.parser.Parser;
import org.jay.parser.parsers.ChooseParser;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Mapper;
import org.w3c.dom.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class RegexParser {


    public static Parser parser2() {
        return start().optional()
                .connect(() -> parser())
                .connect(() -> end().optional());
    }
    public static Parser parser() {
        return token().many().map();
    }

    public static Parser tokenParser() {
        return ChooseParser.choose(
        ).many();
    }

    public static Parser start() {
        return TextParsers.one('^')
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
                        .value(Token.CharToken.builder().type(CharType.DOT).parser(TextParsers.any()).build()).build())),
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
                                        .parser(TextParsers.satisfy((Predicate<Character>) s.get(0)))
                                .build()).build()),
                TextParsers.one('(').ignore()
                        .connect(() -> parser())
                        .connect(() ->TextParsers.one(')').ignore())
                        .map(s -> Token.builder().type(TokenType.GROUP).value(s.get(0)).build()),
                TextParsers.one('\\').ignore()
                        .connect(() -> TextParsers.satisfy(Character::isDigit).map(s -> Character.getNumericValue((Character) s.get(0))))
                        .map(s -> Token.builder().type(TokenType.QUOTE).value(s.get(0)).build()),
                TextParsers.one('|').map(Mapper.replace(Token.builder().type(TokenType.OR))),
                TextParsers.satisfy(c -> !Character.isISOControl(c)).map(s -> Token.builder().type(TokenType.VALID_CHAR)
                        .value(Token.CharToken.builder()
                                .type(CharType.CHAR)
                                .parser(TextParsers.satisfy(ch -> ch == s.get(0)))
                                .build()).build())
        );
    }

    public static Parser escape() {
        return ChooseParser.choose(
                TextParsers.string("\\s").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).predicate(EscapeToken.WHITE.getParser()).build()).build()),
                TextParsers.string("\\S").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.NON_WHITE.getParser()).build()).build()),
                TextParsers.string("\\d").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.DIGIT.getParser()).build()).build()),
                TextParsers.string("\\D").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.NON_DIGIT.getParser()).build()).build()),
                TextParsers.string("\\w").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.WORD.getParser()).build()).build()),
                TextParsers.string("\\W").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.NON_WORD.getParser()).build()).build()),
                TextParsers.string("\\.").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.DOT.getParser()).build()).build()),
                TextParsers.string("\\(").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.LEFT_BRACKET.getParser()).build()).build()),
                TextParsers.string("\\)").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.RIGHT_BRACKET.getParser()).build()).build()),
                TextParsers.string("\\[").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.LEFT_SQUARE_BRACKET.getParser()).build()).build()),
                TextParsers.string("\\]").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.RIGHT_SQUARE_BRACKET.getParser()).build()).build()),
                TextParsers.string("\\\\").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.BACKSLASH.getParser()).build()).build()),
                TextParsers.string("\\+").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.PLUS.getParser()).build()).build()),
                TextParsers.string("\\*").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.STAR.getParser()).build()).build()),
                TextParsers.string("\\?").map(s -> Token.builder().type(TokenType.VALID_CHAR).value(Token.CharToken.builder().type(CharType.ESCAPE).parser(EscapeToken.QUESTION_MARK.getParser()).build()).build())
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
    public static Function<List, ?> tokensMapper() {

    }

}
