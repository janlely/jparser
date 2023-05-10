package io.github.janlely.jparser.impl.json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * define of json object
 */
public class JsonObject {
    /**
     * value map
     */
    Map<String, JsonValue> json;

    /**
     * constructor
     */
    public JsonObject() {
        this.json = new LinkedHashMap<>();
    }

    /**
     * @param members the object members
     * @return json object
     */
    public JsonObject addAll(List<JsonMember> members) {
        for (JsonMember member : members) {
            this.json.put(member.getKey(), member.getValue());
        }
        return this;
    }
}
