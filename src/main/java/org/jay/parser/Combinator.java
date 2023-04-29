package org.jay.parser;

import org.jay.parser.util.Buffer;

public class Combinator {
    public static Parser choose(Parser parser, Parser ...others) {

        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                Result result = parser.parse(buffer);
                if (result.isSuccess()) {
                    return result;
                }
                for (Parser other : others) {
                    Result tmp = other.parse(buffer);
                    if (tmp.isSuccess()) {
                        return tmp;
                    }
                }
                return Result.builder()
                        .errorMsg("No suitable Parser to choose")
                        .build();
            }
        };
    }
}
