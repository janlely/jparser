package org.jay.parser.impl.xml;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class XmlProp {
    private String name;
    private String value;
}
