package io.github.janlely.jparser;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


/**
 * the result type
 */
@Builder
public class Result {

    /**
     * list of result
     */
    @Getter
    private List result;
    /**
     * error message
     */
    String errorMsg;
    /**
     * result length
     */
    @Getter
    int length;

    /**
     * the error position
     */
    @Getter
    int pos;

    /**
     * @return if is success
     */
    public boolean isSuccess() {
        return result != null;
    }

    /**
     * @return is is error
     */
    public boolean isError() {
        return !isSuccess();
    }

    /**
     * @param idx the index
     * @param <T> the type
     * @return the value at idx
     */
    public <T> T get(int idx) {
        return (T) result.get(idx);
    }

    /**
     * @param list the result list to add
     */
    public void addAll(List list) {
        this.result.addAll(list);
    }

    /**
     * @return a empty Result
     */
    public static Result empty() {
        return Result.builder()
                .length(0)
                .result(new ArrayList(0))
                .build();
    }

    /**
     * clear the result
     */
    public void clear() {
        this.result = new ArrayList(0);
    }

    /**
     * @return if is empty
     */
    public boolean isEmpty() {
        return this.result == null || this.result.isEmpty();
    }

    /**
     * @param n update length
     */
    public void incLen(int n) {
        this.length += n;
    }

    /**
     * @param mapper the mapper
     */
    public void map(Function<List, ?> mapper) {
        this.result = Collections.singletonList(mapper.apply(this.result));
    }

    /**
     * @return if reach end
     */
    public static Result reachEnd() {
        return Result.builder()
                .errorMsg("no more data to parse")
                .pos(-1)
                .build();
    }

    /**
     * @return broken result
     */
    public static Result broken() {
        return Result.builder()
                .errorMsg("broken parser")
                .pos(-1)
                .build();

    }
}
