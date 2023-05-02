package org.jay.parser.impl.regex;

import lombok.Getter;
import org.jay.parser.Parser;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.F;

import java.util.function.Predicate;

import static java.util.regex.CharPredicates.DIGIT;

public enum EscapeToken {

    WORD((F.any(Character::isLetterOrDigit, ch -> ch == '_')), //\w [A-z0-9_]
    NON_WORD(F.all(F.not(Character::isISOControl), F.not(F.any(Character::isLetterOrDigit, ch -> ch == '_')))),//\W [^A-z0-9]
    DIGIT(Character::isDigit), //\d [0-9]
    NON_DIGIT(TextParsers.satisfy(F.all(
            F.not(Character::isISOControl),
            F.not(Character::isDigit)
    ))), //\D [^0-9]
    WHITE(TextParsers.satisfy(Character::isWhitespace)), //\s [ \t\r\n\v\f]
    NON_WHITE(TextParsers.satisfy(F.all(
            F.not(Character::isISOControl),
            F.not(Character::isWhitespace)
    ))), //\S [^ \t\r\n\v\f]
    DOT(TextParsers.one('.')), // \. .
    PLUS(TextParsers.one('+')), // \+ +
    STAR(TextParsers.one('*')), // \* *
    QUESTION_MARK(TextParsers.one('?')), //\? ?
    LEFT_BRACKET(TextParsers.one('(')), // \(
    RIGHT_BRACKET(TextParsers.one(')')), // \)
    LEFT_SQUARE_BRACKET(TextParsers.one('[')), // \[
    RIGHT_SQUARE_BRACKET(TextParsers.one(']')), // \]
    BACKSLASH(TextParsers.one('\\')); // \\

    @Getter
    private Predicate<Character> predicate;

    EscapeToken(Predicate<Character> predicate) {
        this.predicate = predicate;
    }
}
