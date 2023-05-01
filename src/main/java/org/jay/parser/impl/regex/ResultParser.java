package org.jay.parser.impl.regex;

import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;

import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class ResultParser extends Parser {

    private int groupId;
    private boolean isGroup = false;
    private List tokens;

    public ResultParser(String label) {
        super(label);
    }

    public ResultParser(String label, Deque<String> queue) {
        super(label, queue);
    }

    public ResultParser(int groupId, List tokens) {
        super(ResultParser.class.getSimpleName());
        if (groupId >= 0) {
            this.groupId = groupId;
            this.isGroup = true;
        }
    }

    @Override
    public Result parse(IBuffer buffer) {
        return null;
    }
}
