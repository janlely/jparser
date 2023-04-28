package org.jay.parser.impl.json;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JsonMember {

    private String key;
    private JsonValue value;
}
