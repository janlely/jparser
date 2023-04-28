package org.jay.parser.impl.json;

import org.jay.parser.Combinator;
import org.jay.parser.Context;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.CharParsers;
import org.jay.parser.parsers.NumberParsers;
import org.jay.parser.parsers.StringParsers;
import org.jay.parser.util.AnyChar;
import org.jay.parser.util.Mapper;
import org.jay.parser.util.Separator;

import java.util.List;

public class JsonParser extends Parser{

    @Override
    public Result parse(Context context) {
        return valueParser().trim().parse(context);
    }

    public static Parser arrayParser() {
        return CharParsers.character(AnyChar.fromAscii('[')).ignore()
                .connect(valueParser().sepBy(Separator.character(',')))
                .connect(CharParsers.character(AnyChar.fromAscii(']')).ignore())
                .map(ary -> JsonValue.builder()
                        .type(JsonType.ARRAY)
                        .value(new JsonArray().addAll(ary))
                        .build());
    }

    public static Parser objectParser() {
        Parser members = member().sepBy(Separator.character(','))
                .map(mbs -> JsonValue.builder()
                        .type(JsonType.OBJECT)
                        .value(new JsonObject().addAll(mbs))
                        .build());
        return CharParsers.character(AnyChar.fromAscii('{')).ignore()
                .connect(members)
                .connect(CharParsers.character(AnyChar.fromAscii('}')).ignore());
    }

    public static Parser member() {
        return stringParser().trim()
                .connect(CharParsers.character(AnyChar.fromAscii(':')).ignore())
                .connect(valueParser())
                .map((List kv) -> JsonMember.builder()
                        .key((String) kv.get(0))
                        .value((JsonValue) kv.get(1))
                        .build());
    }

    public static Parser stringParser() {
        Parser escape = CharParsers.character(AnyChar.fromAscii('\\')).ignore()
                .connect(Combinator.choose(
                        CharParsers.character(AnyChar.fromAscii('"')),
                        CharParsers.character(AnyChar.fromAscii('\\'))
                ));
        Parser charParser = Combinator.choose(
                escape,
                CharParsers.satisfy(c -> c != '"')
        );
        return CharParsers.character(AnyChar.fromAscii('"')).ignore()
                .connect(charParser.many().map(Mapper.toStr()))
                .connect(CharParsers.character(AnyChar.fromAscii('"')).ignore());
    }


    public static Parser nullParser() {
        return StringParsers.string("null", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.NULL)
                        .value("null")
                        .build());
    }

    public static Parser numberParser() {
        return CharParsers.satisfy(c -> Character.isDigit(c) || c == '-' || c == '.' || c == 'e')
                .many()
                .map(Mapper.toStr())
                .map(ss -> JsonValue.builder()
                        .type(JsonType.NUMBER)
                        .value(Double.parseDouble((String) ss.get(0)))
                        .build());

    }

    public static Parser valueParser() {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                char head = context.head();
                switch (head) {
                    case '"' :
                        return stringParser().map(s -> JsonValue.builder()
                                .type(JsonType.STRING)
                                .value(s.get(0))
                                .build()).runParser(context);
                    case '{' :
                        return objectParser().runParser(context);
                    case '[' :
                        return arrayParser().runParser(context);
                    case 'n' :
                    case 'N' :
                        return nullParser().runParser(context);
                    default:
                        return numberParser().runParser(context);
                }
            }
        };
    }
}
