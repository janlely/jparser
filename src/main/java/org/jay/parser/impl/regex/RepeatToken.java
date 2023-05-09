package org.jay.parser.impl.regex;

import lombok.Builder;
import lombok.Data;

/**
 * the repeat token
 */
@Data
@Builder
public class RepeatToken {
    /**
     * repeat type
     */
    private RepeatType type;
    /**
     * repeat value
     */
    private Object value;

    /**
     * @param <T> value type
     * @return the value
     */
    public <T> T getValue() {
        return (T) value;
        }
}
