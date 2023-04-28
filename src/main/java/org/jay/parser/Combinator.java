package org.jay.parser;

import java.util.List;

public class Combinator {
    public static Parser choose(Parser parser, Parser ...others) {

        return new Parser() {
            @Override
            public Result parse(Context context) {
                Result result = parser.parse(context);
                if (result.result != null) {
                    return result;
                }
                for (Parser other : others) {
                    Result tmp = other.parse(context);
                    if (tmp.result != null) {
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
