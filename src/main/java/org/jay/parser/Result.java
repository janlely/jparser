package org.jay.parser;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


@Builder
public class Result {

    @Getter
    private List result;
    String errorMsg;
    int length;

    public boolean isSuccess() {
        return result != null;
    }

    public boolean isError() {
        return !isSuccess();
    }
    public <T> T get(int idx) {
        return (T) result.get(0);
    }

    public void addAll(List l) {
        this.result.addAll(l);
    }

    public static Result empty() {
        return Result.builder()
                .length(0)
                .result(new ArrayList(0))
                .build();
    }

    public boolean isEmpty() {
        return this.result.isEmpty();
    }

    public void map(Function<List, ?> mapper) {
        this.result = List.of(mapper.apply(this.result));
    }
}
