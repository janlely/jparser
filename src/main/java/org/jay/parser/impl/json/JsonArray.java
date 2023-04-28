package org.jay.parser.impl.json;

import java.util.ArrayList;
import java.util.List;

public class JsonArray {
    List<JsonValue> array;

    public JsonArray() {
        this.array = new ArrayList<>();
    }

    public JsonArray addAll(List<JsonValue> values) {
        this.array.addAll(values);
        return this;
    }
}
