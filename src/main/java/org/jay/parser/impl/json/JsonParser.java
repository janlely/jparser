package org.jay.parser.impl.json;

import org.jay.parser.Combinator;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.CharParsers;
import org.jay.parser.parsers.StringParsers;
import org.jay.parser.util.AnyChar;

public class JsonParser extends Parser{

    @Override
    public Result parse(Context context) {
        return null;
    }

    public static Parser arrayParser() {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                return null;
            }
        };
    }

    public static Parser objectParser() {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                return null;
            }
        };
    }

    public static Parser keyParser() {
        Parser escape = CharParsers.character(AnyChar.fromAscii('\\')).ignore()
                .connect(Combinator.choose(
                        CharParsers.character(AnyChar.fromAscii('"')),
                        CharParsers.character(AnyChar.fromAscii('\\'))
                ));
        charParser = Combinator.choose(
                escape,
                CharParsers.satisfy(c -> c != '"')
        ).many().map;
        return CharParsers.character(AnyChar.fromAscii('"')).ignore()
                .connect(Combinator.choose())
    }

    public static Parser nullParser() {
        return StringParsers.string("null", true).map(__ -> "null");
    }
}
