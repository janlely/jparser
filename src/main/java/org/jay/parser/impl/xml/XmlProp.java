package org.jay.parser.impl.xml;

import lombok.Builder;
import lombok.Data;

/**
 * define of XML Property
 */
@Data
@Builder
public class XmlProp {
    /**
     * property name
     */
    private String name;
    /**
     * property value
     */
    private String value;
}
