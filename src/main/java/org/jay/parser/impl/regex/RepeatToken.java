package org.jay.parser.impl.regex;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepeatToken {
    private RepeatType type;
    private Object value;

    public <T> T getValue() {
        return (T) value;
        }
}
