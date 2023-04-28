package org.jay.parser.impl.json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonObject {
    Map<String, JsonValue> json;

    public JsonObject() {
        this.json = new LinkedHashMap<>();
    }

    public JsonObject addAll(List<JsonMember> members) {
        for (JsonMember member : members) {
            this.json.put(member.getKey(), member.getValue());
        }
        return this;
    }
}
