package org.jay.parser.impl.xml;

import org.jay.parser.Combinator;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;
import org.jay.parser.util.Buffer;
import org.jay.parser.util.Mapper;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class XmlParser extends Parser {
    @Override
    public Result parse(Buffer buffer) {
        return nodeParser().connect(() -> TextParsers.eof()).runParser(buffer);
    }

    public Parser nodeParser() {
        return emptyParser().or(() -> fullParser()).trim();
    }

    public Parser contentParser() {
        return contentEscapeParser().or(() ->
                TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '<' && c != '>').trim()
        ).many().map(Mapper.toStr());
    }

    public Parser fullParser() {
        return headParser()
                .connect(() -> nodeParser().some().or(() -> contentParser()))
                .connectWith(result -> {
                    String name = result.<XmlNode>get(0).getName();
                    return TextParsers.string("</").ignore()
                            .connect(() -> TextParsers.string(name).trim())
                            .connect(() -> TextParsers.string(">").ignore())
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


    public Parser headParser() {
        return TextParsers.one('<').ignore()
                .connect(() -> tagParser())
                .connect(() -> TextParsers.one('>').ignore())
                .map(values -> {
                    String name = (String) values.get(0);
                    List<XmlProp> props = (List<XmlProp>) values.stream().skip(1).collect(Collectors.toList());
                    return XmlNode.builder()
                            .props(props)
                            .name(name)
                            .build();
                });
    }


    public Parser emptyParser() {
        return TextParsers.one('<').ignore()
                .connect(() -> tagParser())
                .connect(() -> TextParsers.string("/>").ignore())
                .map(values -> {
                    String name = (String) values.get(0);
                    List<XmlProp> props = (List<XmlProp>) values.stream().skip(1).collect(Collectors.toList());
                    return XmlNode.builder()
                            .props(props)
                            .name(name)
                            .build();
                });
    }

    public Parser tagParser() {
        return nameParser()
                .connect(() -> TextParsers.blank())
                .connect(() -> propParser().sepBy(TextParsers.blank()).optional());
    }

    public Parser tailParser(String name) {
        return TextParsers.string("</").ignore()
                .connect(() -> TextParsers.string(name).trim())
                .connect(() -> TextParsers.string(">").ignore())
                .ignore();
    }

    public Parser nameParser() {
        return TextParsers.satisfy(validName())
                .some().map(Mapper.toStr());
    }

    public Parser propParser() {
        return nameParser()
                .trim()
                .connect(() -> TextParsers.one('=').ignore())
                .connect(() -> propValueParser())
                .map(kv -> XmlProp.builder().name((String) kv.get(0)).value((String) kv.get(1)).build());
    }

    public Parser propValueParser() {
        Parser singleQuote = TextParsers.one('\'').ignore()
                .connect(() -> valueEscapeParser().or(() ->
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '\'')
                ).many().map(Mapper.toStr()))
                .connect(() -> TextParsers.one('\'').ignore());
        Parser doubleQuote = TextParsers.one('"').ignore()
                .connect(() -> valueEscapeParser().or(() ->
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"')
                ).many().map(Mapper.toStr()))
                .connect(() -> TextParsers.one('"').ignore());
        return singleQuote.or(() -> doubleQuote);
    }

    public Parser valueEscapeParser() {
        //TODO more escape char to be added
        return TextParsers.string(" &quot").map(Mapper.replace('"'));
    }

    public Parser contentEscapeParser() {
        return TextParsers.string("&lt;").map(Mapper.replace('<')).or(() ->
                TextParsers.string("&gt;").map(Mapper.replace('<')));
    }
    public Predicate<Character> validName() {
        return c -> Character.isLetterOrDigit(c)
                || Set.of('.','-','_',':').contains(c);
    }
}
