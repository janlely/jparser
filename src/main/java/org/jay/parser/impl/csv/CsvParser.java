package org.jay.parser.impl.csv;

import org.jay.parser.Parser;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Mapper;

public class CsvParser {

    public static Parser fileParser() {
        return lineParser().sepBy(TextParsers.one('\n').ignore());
    }
    public static Parser lineParser() {
        return field().sepBy(TextParsers.one(',').ignore()).trim(false);
    }

    public static Parser field() {
        return fieldCase1().or(() -> fieldCase2());
    }

    public static Parser fieldCase1() {
        Parser escapeParser = TextParsers.one('"').ignore().connect(() -> TextParsers.one('"'));
        return TextParsers.one('"').ignore()
                .connect(() -> escapeParser
                        .or(() -> TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"'))
                        .many().map(Mapper.toStr()))
                .connect(() -> TextParsers.one('"').ignore());
    }

    public static Parser fieldCase2() {
        return TextParsers.satisfy(c -> !Character.isISOControl(c) && c != ',')
                .many().map(Mapper.toStr());
    }
}
