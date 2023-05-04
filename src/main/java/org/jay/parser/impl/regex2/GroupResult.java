package org.jay.parser.impl.regex2;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupResult {

    private int groupId;
    private List value;
}
