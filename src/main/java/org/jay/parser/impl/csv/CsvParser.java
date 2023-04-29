package org.jay.parser.impl.csv;

import org.jay.parser.Combinator;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.Mapper;

public class CsvParser {

    public static Parser fileParser() {
        return lineParser().sepBy(TextParsers.one('\n').ignore());
    }
    public static Parser lineParser() {
        return fieldParser().sepBy(TextParsers.one(',').ignore()).many();
    }

    public static Parser fieldParser() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                if (buffer.remaining() <= 0) {
                    return Result.builder()
                            .errorMsg("no more data to parse")
                            .build();
                }
                byte head = buffer.head();
                switch (head) {
                    case '"':
                        return TextParsers.one('"').ignore()
                                .connect(Combinator.choose(
                                                TextParsers.one('"').ignore().connect(TextParsers.one('"')),
                                                TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"'))
                                        .many().map(Mapper.toStr()))
                                .connect(TextParsers.one('"').ignore()).runParser(buffer);
                    default:
                        return TextParsers.satisfy(c -> !Character.isISOControl(c) && c != ',')
                                .many().map(Mapper.toStr()).runParser(buffer);
                }
            }
        };
    }
}
