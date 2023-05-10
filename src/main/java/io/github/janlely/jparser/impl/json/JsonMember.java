package io.github.janlely.jparser.impl.json;

import lombok.Builder;
import lombok.Data;

/**
 * define of json object member
 */
@Data
@Builder
public class JsonMember {

    /**
     * key
     */
    private String key;
    /**
     * value
     */
    private JsonValue value;
}
