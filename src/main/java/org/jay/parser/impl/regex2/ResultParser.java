package org.jay.parser.impl.regex2;

import org.jay.parser.Parser;
import org.jay.parser.parsers.TextParsers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

public class ResultParser {

    private int groupId;
    private List<Token> tokens;
    private Map<Integer, Parser> groupParsers;


    public ResultParser(int groupId, List<Token> tokens, Map<Integer, Parser> groupParsers) {
        this.groupId = groupId;
        this.tokens = tokens;
        this.groupParsers = groupParsers;
    }

    /**
     * if token is repeat
     * @param type
     * @return
     */
    private boolean isRepeat(TokenType type) {
        return type == TokenType.REPEAT;
    }

    /**
     * only VALID_CHAR,GROUP,QUOTE is repeatable
     * @param type
     * @return
     */
    private boolean isNotRepeatable(TokenType type) {
        return type != TokenType.VALID_CHAR && type != TokenType.GROUP && type != TokenType.QUOTE;

    }

    private List<List<Token>> splitByOr() {
        List<List<Token>> result = new ArrayList<>();
        int start = 0;
        for(int i = 0; i < this.tokens.size(); i++) {
            if (tokens.get(i).type == TokenType.OR && i > start) {
                result.add(this.tokens.subList(start, i));
                start = i + 1;
            }
        }
        if (start < tokens.size()) {
            result.add(this.tokens.subList(start, this.tokens.size()));
        }
        return result;
    }

    private Parser doGenerateParse(List<Token> tokens) {
        //check validation
        for(int i = 0; i < tokens.size(); i++) {
            //REPEAT must be preceded by repeatable token
            if (isRepeat(tokens.get(i).type) && (i < 1 || isNotRepeatable(tokens.get(i-1).type))) {
                throw new InvalidRegexException("unrepeatable token");
            }
        }
        //
        Stack<Token> stack = new Stack<>();
        for (Token token : tokens) {
            stack.push(token);
        }
        return connect(stack);
    }

    private Parser connect(Stack<Token> tokens) {
        Stack<Parser> parserStack = new Stack<>();
        while(!tokens.isEmpty()) {
            Token token = tokens.pop();
            switch (token.type) {
                case REPEAT:
                    if (tokens.isEmpty() || isNotRepeatable(tokens.peek().type)) {
                        return Parser.broken();
                    }
                    parserStack.push(toRepeat((Token.RepeatToken) token.value, tokenParser(tokens.pop())));
                    break;
                case VALID_CHAR:
                case GROUP:
                    parserStack.push(tokenParser(token));
                    break;
                case QUOTE:
                    int gid = (int) token.value;
                    if (!this.groupParsers.containsKey(gid)) {
                        throw new InvalidRegexException("wrong groupId: " + gid);
                    }
                    parserStack.push(this.groupParsers.get(gid));
                    break;
            }
        }
        if (parserStack.isEmpty()) {
            return Parser.empty();
        }
        Parser head = parserStack.pop();
        LinkedList<Parser> tail = new LinkedList<>();
        while(!parserStack.isEmpty()) {
            tail.add(parserStack.pop());
        }
        return Parser.btConnect(true, head, tail);
    }

    public Parser generateParse() {
        //split by |
        Parser parser = Parser.choose(splitByOr().stream()
                .map(this::doGenerateParse).collect(Collectors.toList()))
                .map(s -> (GroupResult.builder().groupId(this.groupId).value(s).build()));
        this.groupParsers.put(groupId, parser);
        return parser;
    }

    public Parser tokenParser(Token token) {
        switch (token.type) {
            case GROUP:
                return new ResultParser(groupId+1, (List<Token>) token.value, this.groupParsers)
                        .generateParse();
            case VALID_CHAR:
                return TextParsers.satisfy(Token.CharToken.class.cast(token.value).getPredicate());
            case REPEAT:
            case START:
            case QUOTE:
            case OR:
            case END:
        }
        throw new InvalidRegexException("unexpected token type: " + token.type.name());
    }

    private Parser toRepeat(Token.RepeatToken token, Parser base) {
        switch (token.getType()) {
            case MANY:
                return base.many();
            case SOME:
                return base.some();
            case RANGE:
                int[] range = token.getValue();
                return base.range(range[0], range[1]);
            case REPEAT:
                return base.repeat((Integer) token.getValue());
            case OPTIONAL:
                return base.optional();
        }
        throw new RuntimeException("unrecognized RepeatToken, type: " + token.getType().name());
    };
}
