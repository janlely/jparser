package io.github.janlely.jparser.comb;

import io.github.janlely.jparser.IBuffer;
import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.parsers.TextParsers;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Parser with back-tracing enabled
 */
public class BacktraceParser extends Parser {

    private boolean greedy;
    private List<Supplier<Parser>> parsers;
    private int head;

    /**
     * Try executing when a better result is found in greedy mode.
     */
    private Runnable runnable;
    public void onResultFound(Runnable runnable) {
        this.runnable = runnable;
    }

    /**
     * @param greedy Greedy or Non-greedy
     * @param parsers Parsers to chain
     */
    public BacktraceParser(boolean greedy, Supplier<Parser> ...parsers) {
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        Collections.addAll(this.parsers, parsers);
        this.runnable = () -> {};
        this.head = 0;
    }

    /**
     * @param greedy Greedy or Non-greedy
     * @param head The first Parser
     * @param tail The remained Parsers
     */
    public BacktraceParser(boolean greedy, Parser head, Supplier<Parser> ...tail) {
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        this.parsers.add(() -> head);
        Collections.addAll(this.parsers, tail);
        this.runnable = () -> {};
        this.head = 0;
    }


    /**
     * @param greedy Greedy or Non-greedy
     * @param parsers Parsers to chain
     */
    public BacktraceParser(boolean greedy, List<Supplier<Parser>> parsers) {
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        this.parsers.addAll(parsers);
        this.runnable = () -> {};
        this.head = 0;
    }

    /**
     * @param greedy Greedy or Non-greedy
     * @param head The first Parser
     * @param tail The remained Parsers
     */
    public BacktraceParser(boolean greedy, Parser head, List<Supplier<Parser>> tail) {
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        this.parsers.add(() -> head);
        this.parsers.addAll(tail);
        this.runnable = () -> {};
        this.head = 0;
    }

    @Override
    public Result parse(IBuffer buffer) {
        if (head == this.parsers.size() - 1) {
            return this.parsers.get(head++).get().runParser(buffer);
        }
        if (head == this.parsers.size()) {
            head = 0;
        }
        LoopObject lp = LoopObject.builder()
                .idx(0)
                .best(Result.broken())
                .build();
        Parser headParser = this.parsers.get(head++).get().chain(() -> TextParsers.eof());
        int thisHead = head;
        while(lp.idx <= buffer.remaining()) {
            IBuffer[] tmp = buffer.splitAt(lp.idx);
            IBuffer left = tmp[0];
            IBuffer right = tmp[1];
            Result headResult = headParser.runParser(left);
            if (headResult.isError()) {
                lp.forward();
                continue;
            }
            head = thisHead;
            Result tailResult = runParser(right);
            if (tailResult.isError()) {
                lp.forward();
                continue;
            }
            if (!greedy) {
                buffer.forward(headResult.getLength() + tailResult.getLength());
                return merge(headResult, tailResult);
            }
            if (!lp.isSuccess()) {
                //the outermost result
                if (thisHead == 1) {
                    runnable.run();
                }
                lp.setBest(merge(headResult, tailResult));
            }
            if (lp.getLen() < headResult.getLength() + tailResult.getLength()) {
                //the outermost result
                if (thisHead == 1) {
                    runnable.run();
                }
                lp.setBest(merge(headResult, tailResult));
            }
            lp.forward();
        }
        buffer.forward(lp.getLen());
        head = this.parsers.size();
        return lp.best;
    }


    /**
     * merge two results
     * @param left The left result
     * @param right The right result
     * @return A new result
     */
    private static Result merge(Result left, Result right) {
        Result result = Result.empty();
        result.addAll(left.getResult());
        result.addAll(right.getResult());
        result.incLen(left.getLength());
        result.incLen(right.getLength());
        return result;
    }

    /**
     * Object when looping
     */
    @Data
    @Builder
    public static class LoopObject {

        /**
         * index
         */
        private int idx;
        /**
         * the best result
         */
        private Result best;

        /**
         * move forward
         */
        public void forward() {
            idx++;
        }

        /**
         * @return if is success
         */
        public boolean isSuccess() {
            return best.isSuccess();
        }

        /**
         * @return the length of result
         */
        public int getLen() {
            return best.getLength();
        }
    }
}
