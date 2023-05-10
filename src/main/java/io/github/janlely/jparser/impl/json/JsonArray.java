package io.github.janlely.jparser.impl.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Define of json array
 */
public class JsonArray {
    List<JsonValue> array;

    /**
     * Constructor
     */
    public JsonArray() {
        this.array = new ArrayList<>();
    }

    /**
     * @param values items
     * @return json array
     */
    public JsonArray addAll(List<JsonValue> values) {
        this.array.addAll(values);
        return this;
    }
}
