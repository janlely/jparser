package org.jay.parser.comb;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.jay.parser.IBuffer;
import org.jay.parser.Parser;
import org.jay.parser.Result;
import org.jay.parser.parsers.TextParsers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class BacktraceParser extends Parser {

    private boolean greedy;
    private List<Supplier<Parser>> parsers;
    private int head;

    /**
     * Try executing when a better result is found in greedy mode.
     */
    @Setter
    private Runnable runnable;

    public BacktraceParser(boolean greedy, Supplier<Parser> ...parsers) {
        super("BacktraceConnector", parsers[0].get().getQueue());
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        Collections.addAll(this.parsers, parsers);
        this.runnable = () -> {};
        this.head = 0;
    }

    public BacktraceParser(boolean greedy, Parser head, Supplier<Parser> ...tail) {
        super("BacktraceConnector", tail[0].get().getQueue());
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        this.parsers.add(() -> head);
        Collections.addAll(this.parsers, tail);
        this.runnable = () -> {};
        this.head = 0;
    }


    public BacktraceParser(boolean greedy, List<Supplier<Parser>> parsers) {
        super("BacktraceConnector", parsers.get(0).get().getQueue());
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        this.parsers.addAll(parsers);
        this.runnable = () -> {};
        this.head = 0;
    }

    public BacktraceParser(boolean greedy, Parser head, List<Supplier<Parser>> tail) {
        super("BacktraceConnector", tail.get(0).get().getQueue());
        this.greedy = greedy;
        this.parsers = new LinkedList<>();
        this.parsers.add(() -> head);
        this.parsers.addAll(tail);
        this.runnable = () -> {};
        this.head = 0;
    }

    @Override
    public Result parse(IBuffer buffer) {
        if (head == this.parsers.size()) {
            head = 0;
        }
        if (head == this.parsers.size() - 1) {
            return this.parsers.get(head++).get().runParser(buffer);
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


    private static Result merge(Result left, Result right) {
        Result result = Result.empty();
        result.addAll(left.getResult());
        result.addAll(right.getResult());
        result.incLen(left.getLength());
        result.incLen(right.getLength());
        return result;
    }

    @Data
    @Builder
    public static class LoopObject {

        private int idx;
        private Result best;
        public void forward() {
            idx++;
        }

        public boolean isSuccess() {
            return best.isSuccess();
        }

        public int getLen() {
            return best.getLength();
        }
    }
}
