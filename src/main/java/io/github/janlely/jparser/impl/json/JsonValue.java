package io.github.janlely.jparser.impl.json;

import lombok.Builder;

/**
 * json value
 */
@Builder
public class JsonValue {
    /**
     * type of json value
     */
    JsonType type;
    /**
     * value
     */
    Object value;
}
