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
                TextParsers.one('.').map(Mapper.replace(Token.builder().type(TokenType.DOT).build())),
                TextParsers.one('+').map(Mapper.replace(Token.builder().type(TokenType.SOME).build())),
                TextParsers.one('*').map(Mapper.replace(Token.builder().type(TokenType.MANY).build())),
                TextParsers.one('{').ignore()
                        .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                        .connect(() -> TextParsers.one(',').ignore())
                        .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                        .connect(() -> TextParsers.one('}').ignore())
                        .map(s -> Token.builder().type(TokenType.RANGE).value(new int[] {(int) s.get(0), (int) s.get(1)}).build()),
                TextParsers.one('{').ignore()
                        .connect(() -> TextParsers.satisfy(Character::isDigit).many().map(Mapper.toStr()).map(Mapper.toInt()))
                        .connect(() -> TextParsers.one('}').ignore())
                        .map(s -> Token.builder().type(TokenType.REPEAT).value(s.get(0)).build()),
                TextParsers.one('?').map(Mapper.replace(Token.builder().type(TokenType.OPTIONAL).build())),
                TextParsers.one('[').ignore()
                        .connect(() -> select())
                        .connect(() -> TextParsers.one(']').ignore())
                        .map(s -> Token.builder().type(TokenType.SELECT).value(s.get(0)).build()),
                TextParsers.one('(').ignore()
                        .connect(() -> parser())
                        .connect(() ->TextParsers.one(')').ignore())
                        .map(s -> Token.builder().type(TokenType.GROUP).value(s.get(0)).build()),
                TextParsers.one('\\').ignore()
                        .connect(() -> TextParsers.satisfy(Character::isDigit).map(s -> Character.getNumericValue((Character) s.get(0))))
                        .map(s -> Token.builder().type(TokenType.QUOTE).value(s.get(0)).build()),
                TextParsers.one('|').map(Mapper.replace(Token.builder().type(TokenType.OR)))
        );
    }

    public static Parser escape() {
        return ChooseParser.choose(
                TextParsers.string("\\s").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.WHITE).build()),
                TextParsers.string("\\S").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.NON_WHITE).build()),
                TextParsers.string("\\d").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.DIGIT).build()),
                TextParsers.string("\\D").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.NON_DIGIT).build()),
                TextParsers.string("\\w").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.WORD).build()),
                TextParsers.string("\\W").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.NON_WORD).build()),
                TextParsers.string("\\.").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.DOT).build()),
                TextParsers.string("\\(").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.LEFT_BRACKET).build()),
                TextParsers.string("\\)").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.RIGHT_BRACKET).build()),
                TextParsers.string("\\[").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.LEFT_SQUARE_BRACKET).build()),
                TextParsers.string("\\]").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.RIGHT_SQUARE_BRACKET).build()),
                TextParsers.string("\\\\").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.BACKSLASH).build()),
                TextParsers.string("\\+").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.PLUS).build()),
                TextParsers.string("\\*").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.STAR).build()),
                TextParsers.string("\\?").map(s -> Token.builder().type(TokenType.ESCAPE).value(EscapeToken.QUESTION_MARK).build())
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
                    return (Predicate<Character>) character -> {
                        return Predicate.class.cast(s.get(1)).test(character);
                    };
                });
    }

    /**
     * map [Token] to a Parser
     * @return
     */
    public static Function<List, ?> tokensMapper() {

    }

}
