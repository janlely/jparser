package org.jay.parser;

import lombok.Getter;

import java.util.List;

public abstract class Constructor {
    @Getter
    private String desc;
    public abstract Object construct(List args);
}
