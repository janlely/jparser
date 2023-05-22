package io.github.janlely.jparser;

import io.github.janlely.jparser.parsers.NumberParsers;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Buffer;
import org.junit.Test;

public class Calculator {

    @Test
    public void testCalc() {
        Result result = expr().parse(Buffer.builder().data("(1+2)*3-(4*2)".getBytes()).build());
        assert result.<Double>get(0).compareTo(1.0) == 0;
        result = expr().parse(Buffer.builder().data("1+2*3-(4*2)".getBytes()).build());
        assert result.<Double>get(0).compareTo(-1.0) == 0;
    }

    public Parser expr() {
        return Parser.choose(
                () -> term().chain(TextParsers.one('+').ignore())
                        .chain(() -> expr()).map(s -> (double)s.get(0) + (double)s.get(1)),
                () -> term().chain(TextParsers.one('-').ignore())
                        .chain(() -> expr()).map(s -> (double)s.get(0) - (double)s.get(1)),
                () -> term()
        );
    }

    public Parser term() {
        return Parser.choose(
                () -> factor().chain(TextParsers.one('*').trim(false).ignore())
                        .chain(() -> term()).map(s -> (double)s.get(0) * (double)s.get(1)),
                () -> factor().chain(TextParsers.one('/').trim(false).ignore())
                        .chain(() -> term()).map(s -> (double)s.get(0) / (double)s.get(1)),
                () -> factor()
        );
    }

    public Parser factor() {
        return Parser.choose(
                TextParsers.one('(').ignore()
                        .chain(() -> expr())
                        .chain(TextParsers.one(')').ignore()),
                number()
        );
    }

    public Parser number() {
        return NumberParsers.anyDoubleStr();
    }
}

