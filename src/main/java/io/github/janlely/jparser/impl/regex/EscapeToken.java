package io.github.janlely.jparser.impl.regex;

import lombok.Getter;
import io.github.janlely.jparser.util.F;

import java.util.function.Predicate;


/**
 * escape token
 */
public enum EscapeToken {

    /**
     * \w
     */
    WORD((F.any(Character::isLetterOrDigit, ch -> ch == '_'))), //\w [A-z0-9_]
    /**
     * \W
     */
    NON_WORD(F.all(F.not(Character::isISOControl), F.not(F.any(Character::isLetterOrDigit, ch -> ch == '_')))),//\W [^A-z0-9]
    /**
     * \d
     */
    DIGIT(Character::isDigit), //\d [0-9]
    /**
     * \D
     */
    NON_DIGIT(F.all(F.not(Character::isISOControl), F.not(Character::isDigit))), //\D [^0-9]
    /**
     * \s
     */
    WHITE(Character::isWhitespace), //\s [ \t\r\n\v\f]
    /**
     * \S
     */
    NON_WHITE(F.all(F.not(Character::isISOControl), F.not(Character::isWhitespace))), //\S [^ \t\r\n\v\f]
    /**
     * \.
     */
    DOT(ch -> ch == '.'), // \. .
    /**
     * \+
     */
    PLUS(ch -> ch == '+'), // \+ +
    /**
     * \*
     */
    STAR(ch -> ch == '*'), // \* *
    /**
     * \?
     */
    QUESTION_MARK(ch -> ch == '?'), //\? ?
    /**
     * \(
     */
    LEFT_BRACKET(ch -> ch == '('), // \(
    /**
     * \)
     */
    RIGHT_BRACKET(ch -> ch == ')'), // \)
    /**
     * \[
     */
    LEFT_SQUARE_BRACKET(ch -> ch == '['), // \[
    /**
     * \]
     */
    RIGHT_SQUARE_BRACKET(ch -> ch == ']'), // \]
    /**
     * \\
     */
    BACKSLASH(ch -> ch == '\\'); // \\

    /**
     * the predicate
     */
    @Getter
    private Predicate<Character> predicate;

    /**
     * @param predicate the predicate
     */
    EscapeToken(Predicate<Character> predicate) {
        this.predicate = predicate;
    }
}
