package org.jay.parser.parsers;

import org.jay.parser.Parser;

public class ChooseParser {

    /**
     * choose a Parser from array of Parser
     * @param parsers
     * @return
     */
    public static Parser choose(Parser ...parsers) {
        Parser parser = Parser.empty();
        for (Parser p : parsers) {
            parser = parser.or(() -> p);
        }
        return parser;
    }
}
