package org.jay.parser.impl.json;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonObject {
    Map<String, JsonValue> json;

    public JsonObject() {
        this.json = new LinkedHashMap<>();
    }
}
