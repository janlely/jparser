package org.jay.parser.impl.json;

import lombok.Builder;

@Builder
public class JsonValue {
    JsonType type;
    Object value;
}
