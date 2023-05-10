package io.github.janlely.jparser.impl.xml;

import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Mapper;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * XMl Parser
 */
public class XmlParser {
    /**
     * @return the final Parser
     */
    public static Parser parser() {
        return nodeParser().chain(() -> TextParsers.eof());
    }

    /**
     * Parse XmlNode
     * @return Node Parser
     */
    public static Parser nodeParser() {
        return emptyParser().or(() -> fullParser()).trim(true);
    }

    /**
     * Parse content
     * @return Content Parser
     */
    public static Parser contentParser() {
        return contentEscapeParser().or(() ->
                TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '<' && c != '>').trim(true)
        ).many().map(Mapper.toStr());
    }

    /**
     * Parse a full xml
     * @return Full XML Parser
     */
    public static Parser fullParser() {
        return headParser()
                .chain(() -> nodeParser().some().or(() -> contentParser()))
                .chainWith(result -> {
                    String name = result.<XmlNode>get(0).getName();
                    return TextParsers.string("</").ignore()
                            .chain(() -> TextParsers.string(name).trim(true))
                            .chain(() -> TextParsers.string(">").ignore())
                            .ignore();
                }).map(values -> {
                    if (values.size() == 1) {
                        return values.get(0);
                    }
                    if (values.get(1) instanceof XmlNode) {
                        XmlNode.class.cast(values.get(0))
                                .initChildren()
                                .addAll(values.subList(1,values.size()));
                        return values.get(0);
                    }
                    XmlNode.class.cast(values.get(0)).setContent((String) values.get(1));
                    return values.get(0);
                });
    }


    /**
     * Parse head
     * @return Head Parser
     */
    public static Parser headParser() {
        return TextParsers.one('<').ignore()
                .chain(() -> tagParser())
                .chain(() -> TextParsers.one('>').ignore())
                .map(values -> {
                    String name = (String) values.get(0);
                    List<XmlProp> props = (List<XmlProp>) values.stream().skip(1).collect(Collectors.toList());
                    return XmlNode.builder()
                            .props(props)
                            .name(name)
                            .build();
                });
    }


    /**
     * Parse empty tag
     * @return Empty tag Parser
     */
    public static Parser emptyParser() {
        return TextParsers.one('<').ignore()
                .chain(() -> tagParser())
                .chain(() -> TextParsers.string("/>").ignore())
                .map(values -> {
                    String name = (String) values.get(0);
                    List<XmlProp> props = (List<XmlProp>) values.stream().skip(1).collect(Collectors.toList());
                    return XmlNode.builder()
                            .props(props)
                            .name(name)
                            .build();
                });
    }

    /**
     * Parse tag: use by headParser and emptyParser
     * @return Tag Parser
     */
    public static Parser tagParser() {
        return nameParser()
                .chain(() -> TextParsers.spaces())
                .chain(() -> propParser().sepBy(TextParsers.spaces()).optional());
    }

    /**
     * Parse tag name or prop name
     * @return Name Parser
     */
    public static Parser nameParser() {
        return TextParsers.satisfy(validName())
                .some().map(Mapper.toStr());
    }

    /**
     * Parse XmlProp
     * @return Property Parser
     */
    public static Parser propParser() {
        return nameParser()
                .trim(true)
                .chain(() -> TextParsers.one('=').ignore())
                .chain(() -> propValueParser())
                .map(kv -> XmlProp.builder().name((String) kv.get(0)).value((String) kv.get(1)).build());
    }

    /**
     * Parse prop value
     * @return property value Parser
     */
    public static Parser propValueParser() {
        Parser singleQuote = TextParsers.one('\'').ignore()
                .chain(() -> valueEscapeParser().or(() ->
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '\'')
                ).many().map(Mapper.toStr()))
                .chain(() -> TextParsers.one('\'').ignore());
        Parser doubleQuote = TextParsers.one('"').ignore()
                .chain(() -> valueEscapeParser().or(() ->
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"')
                ).many().map(Mapper.toStr()))
                .chain(() -> TextParsers.one('"').ignore());
        return singleQuote.or(() -> doubleQuote);
    }

    /**
     * Parse escape character in prop value
     * @return Value escape Parser
     */
    public static Parser valueEscapeParser() {
        //TODO more escape char to be added
        return TextParsers.string(" &quot").map(Mapper.replace('"'));
    }

    /**
     * Parse escape character in content
     * @return Content escape Parser
     */
    public static Parser contentEscapeParser() {
        return TextParsers.string("&lt;").map(Mapper.replace('<')).or(() ->
                TextParsers.string("&gt;").map(Mapper.replace('<')));
    }

    /**
     * Check if a character is allowed in XML tag names
     * @return predicate of valid name
     */
    public static Predicate<Character> validName() {
        return c -> Character.isLetterOrDigit(c)
                || Set.of('.','-','_',':').contains(c);
    }
}
