package org.jay.parser.impl.regex;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class ResultParser {

    private AtomicInteger groupId;
    private boolean isGroup;
    private List<Token> tokens;
    private Stack<Parser> parsers;


    public ResultParser(boolean isGroup, AtomicInteger groupId, List<Token> tokens) {
        this.isGroup = isGroup;
        if (this.isGroup) {
            this.groupId = groupId;
        }
        this.tokens = tokens;
    }

    public Parser generateParse() {
        for (Token token : tokens) {
            this.parsers.push(tokenParser(token));
        }

        if (parser.isEmpty()) {
            return Parser.empty();
        }
        if (parser.get().isKiller()) {
            return parser.get();
        }
        return Parser.scan(TextParsers.skip(1), parser.get());
    }

    public Parser tokenParser(Token token) {
        switch (token.type) {
            case START:
                return Parser.empty().killer();
            case END:
                return TextParsers.eof();
            case GROUP:
                return new ResultParser(true, groupId, (List<Token>)token.value).generateParse();
            case VALID_CHAR:
                return TextParsers.one(Token.CharToken.class.cast(token.value));
            case ESCAPE:
                return EscapeToken.class.cast(token.value).getParser();
            case RANGE:
        }
    }
}
