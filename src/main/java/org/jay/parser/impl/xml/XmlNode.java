package org.jay.parser.impl.xml;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * define of xml node
 */
@Builder
@Data
public class XmlNode {
    /**
     * node name
     */
    private String name;
    /**
     * node properties
     */
    private List<XmlProp> props;
    /**
     * children
     */
    private Map<String, List<XmlNode>> children;
    /**
     * node content
     */
    private String content;

    /**
     * @return this
     */
    public XmlNode initChildren() {
        if (children == null) {
            this.children = new LinkedHashMap<>();
        }
        return this;
    }


    /**
     * @param nodes add all nodes
     */
    public void addAll(List<XmlNode> nodes) {
        for (XmlNode node : nodes) {
            if (!children.containsKey(node.name)) {
                children.put(node.name, new ArrayList<>());
            }
            children.get(node.name).add(node);
        }
    }
}
