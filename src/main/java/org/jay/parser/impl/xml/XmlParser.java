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
import java.util.stream.Collectors;

public class XmlParser extends Parser {
    @Override
    public Result parse(Buffer buffer) {
        return nodeParser().connect(TextParsers.eof()).runParser(buffer);
    }

//    public static Parser nodeParser() {
//        return emptyParser().or(fullParser()).trim();
//    }

    public static Parser nodeParser() {
        return new Parser() {
            @Override
            public Result parse(Buffer buffer) {
                return emptyParser().or(fullParser()).trim().runParser(buffer);
            }
        }.trim();
    }

    public static Parser contentParser() {
        return Combinator.choose(
                contentEscapeParser(),
                TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '<' && c != '>').trim()
        ).many().map(Mapper.toStr());
    }

    public static Parser fullParser() {
        return headParser()
                .connect(nodeParser().some().or(contentParser()))
                .connectWith(result -> {
                    String name = result.<XmlNode>get(0).getName();
                    return TextParsers.string("</").ignore()
                            .connect(TextParsers.string(name).trim())
                            .connect(TextParsers.string(">").ignore())
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


    public static Parser headParser() {
        return TextParsers.one('<').ignore()
                .connect(tagParser())
                .connect(TextParsers.one('>').ignore())
                .map(values -> {
                    String name = (String) values.get(0);
                    List<XmlProp> props = (List<XmlProp>) values.stream().skip(1).collect(Collectors.toList());
                    return XmlNode.builder()
                            .props(props)
                            .name(name)
                            .build();
                });
    }


    public static Parser emptyParser() {
        return TextParsers.one('<').ignore()
                .connect(tagParser())
                .connect(TextParsers.string("/>").ignore())
                .map(values -> {
                    String name = (String) values.get(0);
                    List<XmlProp> props = (List<XmlProp>) values.stream().skip(1).collect(Collectors.toList());
                    return XmlNode.builder()
                            .props(props)
                            .name(name)
                            .build();
                });
    }

    public static Parser tagParser() {
        return nameParser()
                .connect(TextParsers.blank())
                .connect(propParser().sepBy(TextParsers.blank()).optional());
    }

    public static Parser tailParser(String name) {
        return TextParsers.string("</").ignore()
                .connect(TextParsers.string(name).trim())
                .connect(TextParsers.string(">").ignore())
                .ignore();
    }

    public static Parser nameParser() {
        return TextParsers.satisfy(validName())
                .some().map(Mapper.toStr());
    }

    public static Parser propParser() {
        return nameParser()
                .trim()
                .connect(TextParsers.one('=').ignore())
                .connect(propValueParser())
                .map(kv -> XmlProp.builder().name((String) kv.get(0)).value((String) kv.get(1)).build());
    }

    public static Parser propValueParser() {
        Parser singleQuote = TextParsers.one('\'').ignore()
                .connect(Combinator.choose(
                        valueEscapeParser(),
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '\'')
                ).many().map(Mapper.toStr()))
                .connect(TextParsers.one('\'').ignore());
        Parser doubleQuote = TextParsers.one('"').ignore()
                .connect(Combinator.choose(
                        valueEscapeParser(),
                        TextParsers.satisfy(c -> !Character.isISOControl(c) && c != '"')
                ).many().map(Mapper.toStr()))
                .connect(TextParsers.one('"').ignore());
        return singleQuote.or(doubleQuote);
    }

    public static Parser valueEscapeParser() {
        //TODO more escape char to be added
        return TextParsers.string(" &quot").map(Mapper.replace('"'));
    }

    public static Parser contentEscapeParser() {
        return Combinator.choose(
                TextParsers.string("&lt;").map(Mapper.replace('<')),
                TextParsers.string("&gt;").map(Mapper.replace('<'))
        );
    }
    public static Predicate<Character> validName() {
        return c -> Character.isLetterOrDigit(c)
                || Set.of('.','-','_',':').contains(c);
    }
}
