package io.github.janlely.jparser;

import io.github.janlely.jparser.impl.xml.XmlNode;
import io.github.janlely.jparser.impl.xml.XmlParser;
import io.github.janlely.jparser.util.Buffer;
import org.junit.Test;

public class XmlParserTests {

    @Test
    public void testTag() {
        Result result1 = XmlParser.tagParser().runParser(Buffer.builder()
                        .data("div data-hidden=\"true\" class=\"PageWithSidebarLayout_overlay__c0mlT\"".getBytes())
                .build());
        assert result1.isSuccess();
    }

    @Test
    public void testHead() {
        Result result1 = XmlParser.headParser().runParser(Buffer.builder()
                .data("<div data-hidden=\"true\" class=\"PageWithSidebarLayout_overlay__c0mlT\">".getBytes())
                .build());
        assert result1.isSuccess();
        assert result1.<XmlNode>get(0).getName().equals("div");
        assert result1.<XmlNode>get(0).getProps().get(0).getName().equals("data-hidden");
        assert result1.<XmlNode>get(0).getProps().get(0).getValue().equals("true");
    }

    @Test
    public void testEmpty() {
        Result result1 = XmlParser.emptyParser().runParser(Buffer.builder()
                        .data("<node />".getBytes())
                .build());
        assert result1.isSuccess();
    }
    @Test
    public void testNode() {
        String src = "<note hello=\"world\">\n" +
                       "<to>Tove</to>\n" +
                       "<from>Jani</from>\n" +
                       "<heading>Reminder</heading>\n" +
                       "<body>Don't forget me this weekend!</body>\n" +
                     "</note>";
        Result result1 = XmlParser.parser().runParser(Buffer.builder()
                .data(src.getBytes())
                .build());
        assert result1.isSuccess();
    }
}
