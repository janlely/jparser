package org.jay.parser.impl.xml;

import lombok.Builder;
import lombok.Data;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Builder
@Data
public class XmlNode {
    private String name;
    private List<XmlProp> props;
    private Map<String, List<XmlNode>> children;
    private String content;

    public XmlNode initChildren() {
        if (children == null) {
            this.children = new LinkedHashMap<>();
        }
        return this;
    }


    public void addAll(List<XmlNode> nodes) {
        for (XmlNode node : nodes) {
            if (!children.containsKey(node.name)) {
                children.put(node.name, new ArrayList<>());
            }
            children.get(node.name).add(node);
        }
    }
}
