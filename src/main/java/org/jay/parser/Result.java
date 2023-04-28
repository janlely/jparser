package org.jay.parser;

import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Builder
public class Result {

    @Getter
    List result;
    String errorMsg;
    int length;
}
