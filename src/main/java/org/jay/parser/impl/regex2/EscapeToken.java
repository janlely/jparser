package org.jay.parser.impl.regex2;

import lombok.Getter;
import org.jay.parser.util.F;

import java.util.function.Predicate;


public enum EscapeToken {

    WORD((F.any(Character::isLetterOrDigit, ch -> ch == '_'))), //\w [A-z0-9_]
    NON_WORD(F.all(F.not(Character::isISOControl), F.not(F.any(Character::isLetterOrDigit, ch -> ch == '_')))),//\W [^A-z0-9]
    DIGIT(Character::isDigit), //\d [0-9]
    NON_DIGIT(F.all(F.not(Character::isISOControl), F.not(Character::isDigit))), //\D [^0-9]
    WHITE(Character::isWhitespace), //\s [ \t\r\n\v\f]
    NON_WHITE(F.all(F.not(Character::isISOControl), F.not(Character::isWhitespace))), //\S [^ \t\r\n\v\f]
    DOT(ch -> ch == '.'), // \. .
    PLUS(ch -> ch == '+'), // \+ +
    STAR(ch -> ch == '*'), // \* *
    QUESTION_MARK(ch -> ch == '?'), //\? ?
    LEFT_BRACKET(ch -> ch == '('), // \(
    RIGHT_BRACKET(ch -> ch == ')'), // \)
    LEFT_SQUARE_BRACKET(ch -> ch == '['), // \[
    RIGHT_SQUARE_BRACKET(ch -> ch == ']'), // \]
    UP_ARROW(ch -> ch == '^'), // ^
    DOLLAR(ch -> ch == '$'), // $
    BACKSLASH(ch -> ch == '\\'); // \\

    @Getter
    private Predicate<Character> predicate;

    EscapeToken(Predicate<Character> predicate) {
        this.predicate = predicate;
    }
}
