package io.github.janlely.jparser.impl.json;

import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Mapper;

import java.util.List;

/**
 * Json Parser
 */
public class JsonParser {

    /**
     * jsonParser + eof
     * @return The final Parser
     */
    public static Parser parser() {
        return jsonParser().chain(() -> TextParsers.eof());
    }

    /**
     * jsonParser
     * @return json value Parser
     */
    public static Parser jsonParser() {
        return stringParser()
                .or(() -> objectParser().trim(true))
                .or(() -> arrayParser().trim(true))
                .or(() -> nullParser().trim(true))
                .or(() -> boolParser().trim(true))
                .or(() -> numberParser().trim(true))
                .trim(true);
    }

    /**
     * parse json array
     * @return json array Parser
     */
    public static Parser arrayParser() {
        return TextParsers.one('[').ignore()
                .chain(() -> jsonParser().sepBy(TextParsers.one(',').ignore()))
                .chain(() -> TextParsers.one(']').ignore())
                .map(ary -> JsonValue.builder()
                        .type(JsonType.ARRAY)
                        .value(new JsonArray().addAll(ary))
                        .build());
    }

    /**
     * parse json object
     * @return json object Parser
     */
    public static Parser objectParser() {
        return TextParsers.one('{').ignore()
                .chain(() -> membersParser())
                .chain(() -> TextParsers.one('}').ignore());
    }

    /**
     * parse one member
     * @return json members Parser
     */
    public static Parser membersParser() {
        return memberParser().sepBy(TextParsers.one(',').ignore())
                .map(mbs -> JsonValue.builder()
                        .type(JsonType.OBJECT)
                        .value(new JsonObject().addAll(mbs))
                        .build());
    }

    /**
     * parse member of json object
     * @return json member Parser
     */
    public static Parser memberParser() {
        return keyParser().trim(true)
                .chain(() -> TextParsers.one(':').ignore())
                .chain(() -> jsonParser())
                .map((List kv) -> JsonMember.builder()
                        .key((String) kv.get(0))
                        .value((JsonValue) kv.get(1))
                        .build());
    }

    /**
     * parse json string
     * @return json key Parser
     */
    public static Parser keyParser() {
        return TextParsers.one('"').ignore()
                .chain(() -> charParser().many().map(Mapper.toStr()))
                .chain(() -> TextParsers.one('"').ignore());
    }

    /**
     * @return character Parser of json key
     */
    public static Parser charParser() {
        Parser escape = TextParsers.one('\\').ignore()
                .chain(() -> TextParsers.one('"')
                        .or(() -> TextParsers.one('\\')));
        return escape.or(() -> TextParsers.satisfy(c -> c != '"'));
    }

    /**
     * @return key Parser composed with double quote
     */
    public static Parser stringParser() {
        return keyParser().map(s -> JsonValue.builder()
                        .type(JsonType.STRING)
                        .value(s.get(0))
                        .build())
                .trim(true);
    }

    /**
     * parse json null
     * @return json null Parser
     */
    public static Parser nullParser() {
        return TextParsers.string("null", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.NULL)
                        .value("null")
                        .build());
    }

    /**
     * parse json number
     * @return json number Parser
     */
    public static Parser numberParser() {
        return TextParsers.satisfy(c -> Character.isDigit(c) || c == '-' || c == '.' || c == 'e')
                .some()
                .map(Mapper.toStr())
                .map(ss -> JsonValue.builder()
                        .type(JsonType.NUMBER)
                        .value(Double.parseDouble((String) ss.get(0)))
                        .build());
    }

    /**
     * parse json bool
     * @return json bool Parser
     */
    public static Parser boolParser() {
        Parser trueValue = TextParsers.string("true", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.BOOL)
                        .value(true)
                        .build());
        Parser falseValue = TextParsers.string("false", true).map(__ ->
                JsonValue.builder()
                        .type(JsonType.BOOL)
                        .value(false)
                        .build());
        return trueValue.or(() -> falseValue);
    }

}
