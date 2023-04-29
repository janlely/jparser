package org.jay.parser.impl.json;

import org.jay.parser.Combinator;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.Mapper;

import java.util.List;

public class JsonParser extends Parser{

    @Override
    public Result parse(Buffer buffer) {
        return valueParser().trim().parse(buffer);
    }

    public static Parser arrayParser() {
        return TextParsers.one('[').ignore()
                .connect(valueParser().sepBy(TextParsers.one(',').ignore()))
                .connect(TextParsers.one(']').ignore())
                .map(ary -> JsonValue.builder()
                        .type(JsonType.ARRAY)
                        .value(new JsonArray().addAll(ary))
                        .build());
    }

    public static Parser objectParser() {
        Parser members = member().sepBy(TextParsers.one(',').ignore())
                .map(mbs -> JsonValue.builder()
                        .type(JsonType.OBJECT)
                        .value(new JsonObject().addAll(mbs))
                        .build());
        return TextParsers.one('{').ignore()
                .connect(members)
                .connect(TextParsers.one('}').ignore());
    }

    public static Parser member() {
        return stringParser().trim()
                .connect(TextParsers.one(':').ignore())
                .connect(valueParser())
                .map((List kv) -> JsonMember.builder()
                        .key((String) kv.get(0))
                        .value((JsonValue) kv.get(1))
                        .build());
    }

    public static Parser stringParser() {
        Parser escape = TextParsers.one('\\').ignore()
                .connect(Combinator.choose(
                        TextParsers.one('"'),
                        TextParsers.one('\\')
                ));
        Parser charParser = Combinator.choose(
                escape,
                TextParsers.satisfy(c -> c != '"')
        );
        return TextParsers.one('"').ignore()
                .connect(charParser.many().map(Mapper.toStr()))
                .connect(TextParsers.one('"').ignore());
    }


    public static Parser nullParser() {
        return TextParsers.string("null", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.NULL)
                        .value("null")
                        .build());
    }

    public static Parser numberParser() {
        return TextParsers.satisfy(c -> Character.isDigit(c) || c == '-' || c == '.' || c == 'e')
                .many()
                .map(Mapper.toStr())
                .map(ss -> JsonValue.builder()
                        .type(JsonType.NUMBER)
                        .value(Double.parseDouble((String) ss.get(0)))
                        .build());

    }

    public static Parser boolParser() {
        Parser trueValue = TextParsers.string("true", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.NULL)
                        .value(true)
                        .build());
        Parser falseValue = TextParsers.string("false", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.NULL)
                        .value(false)
                        .build());
        return Combinator.choose(trueValue, falseValue);
    }

    public static Parser valueParser() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                byte head = buffer.head();
                switch (head) {
                    case '"' :
                        return stringParser().map(s -> JsonValue.builder()
                                .type(JsonType.STRING)
                                .value(s.get(0))
                                .build()).trim().runParser(buffer);
                    case '{' :
                        return objectParser().trim().runParser(buffer);
                    case '[' :
                        return arrayParser().trim().runParser(buffer);
                    case 'n' :
                    case 'N' :
                        return nullParser().trim().runParser(buffer);
                    case 't':
                    case 'T':
                    case 'f':
                    case 'F':
                        return boolParser().trim().runParser(buffer);
                    default:
                        return numberParser().trim().runParser(buffer);
                }
            }
        };
    }
}
