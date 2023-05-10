package io.github.janlely.jparser.impl.csv;

import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Mapper;

/**
 * CSV Parser
 */
public class CsvParser {

    /**
     * @return CSV file Parser
     */
    public static Parser fileParser() {
        return lineParser().sepBy(TextParsers.one('\n').ignore());
    }

    /**
     * @return CSV line Parser
     */
    public static Parser lineParser() {
        return field().sepBy(TextParsers.one(',').ignore()).trim(false);
    }

    /**
     * @return CSV field Parser
     */
    public static Parser field() {
        return fieldCase1().or(() -> fieldCase2());
    }

    /**
     * @return field case 1
     */
    public static Parser fieldCase1() {
        Parser escapeParser = TextParsers.one('"').ignore().chain(() -> TextParsers.one('"'));
        return TextParsers.one('"').ignore()
                .chain(() -> escapeParser
                        .or(() -> TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"'))
                        .many().map(Mapper.toStr()))
                .chain(() -> TextParsers.one('"').ignore());
    }

    /**
     * @return field case 1
     */
    public static Parser fieldCase2() {
        return TextParsers.satisfy(c -> !Character.isISOControl(c) && c != ',')
                .many().map(Mapper.toStr());
    }
}
