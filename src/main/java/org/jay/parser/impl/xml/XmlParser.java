package org.jay.parser.impl.xml;

import org.jay.parser.Parser;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Mapper;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class XmlParser {
    public static Parser parser() {
        return nodeParser().concat(() -> TextParsers.eof());
    }

    /**
     * Parse XmlNode
     * @return
     */
    public static Parser nodeParser() {
        return emptyParser().or(() -> fullParser()).trim(true);
    }

    /**
     * Parse content: <tag> ${content} </tag>
     * @return
     */
    public static Parser contentParser() {
        return contentEscapeParser().or(() ->
                TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '<' && c != '>').trim(true)
        ).many().map(Mapper.toStr());
    }

    /**
     * Parse a full xml: <tag> content or children node </tag>
     * @return
     */
    public static Parser fullParser() {
        return headParser()
                .concat(() -> nodeParser().some().or(() -> contentParser()))
                .concatWith(result -> {
                    String name = result.<XmlNode>get(0).getName();
                    return TextParsers.string("</").ignore()
                            .concat(() -> TextParsers.string(name).trim(true))
                            .concat(() -> TextParsers.string(">").ignore())
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
     * Parse head: <name prop="value">
     * @return
     */
    public static Parser headParser() {
        return TextParsers.one('<').ignore()
                .concat(() -> tagParser())
                .concat(() -> TextParsers.one('>').ignore())
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
     * Parse empty tag: <name prop="value" />
     * @return
     */
    public static Parser emptyParser() {
        return TextParsers.one('<').ignore()
                .concat(() -> tagParser())
                .concat(() -> TextParsers.string("/>").ignore())
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
     * @return
     */
    public static Parser tagParser() {
        return nameParser()
                .concat(() -> TextParsers.spaces())
                .concat(() -> propParser().sepBy(TextParsers.spaces()).optional());
    }

    /**
     * Parse tag name or prop name
     * @return
     */
    public static Parser nameParser() {
        return TextParsers.satisfy(validName())
                .some().map(Mapper.toStr());
    }

    /**
     * Parse XmlProp
     * @return
     */
    public static Parser propParser() {
        return nameParser()
                .trim(true)
                .concat(() -> TextParsers.one('=').ignore())
                .concat(() -> propValueParser())
                .map(kv -> XmlProp.builder().name((String) kv.get(0)).value((String) kv.get(1)).build());
    }

    /**
     * Parse prop value
     * @return
     */
    public static Parser propValueParser() {
        Parser singleQuote = TextParsers.one('\'').ignore()
                .concat(() -> valueEscapeParser().or(() ->
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '\'')
                ).many().map(Mapper.toStr()))
                .concat(() -> TextParsers.one('\'').ignore());
        Parser doubleQuote = TextParsers.one('"').ignore()
                .concat(() -> valueEscapeParser().or(() ->
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"')
                ).many().map(Mapper.toStr()))
                .concat(() -> TextParsers.one('"').ignore());
        return singleQuote.or(() -> doubleQuote);
    }

    /**
     * Parse escape character in prop value
     * @return
     */
    public static Parser valueEscapeParser() {
        //TODO more escape char to be added
        return TextParsers.string(" &quot").map(Mapper.replace('"'));
    }

    /**
     * Parse escape character in content
     * @return
     */
    public static Parser contentEscapeParser() {
        return TextParsers.string("&lt;").map(Mapper.replace('<')).or(() ->
                TextParsers.string("&gt;").map(Mapper.replace('<')));
    }

    /**
     * Check if a character is allowed in XML tag names
     * @return
     */
    public static Predicate<Character> validName() {
        return c -> Character.isLetterOrDigit(c)
                || Set.of('.','-','_',':').contains(c);
    }
}
