package org.jay.parser.util;

import java.util.Arrays;
import java.util.function.Predicate;

public class F {

    public static <T> Predicate<T> not(Predicate<T> p) {
        return t -> !p.test(t);
    }

    public static <T> Predicate<T> noneOf(Predicate<T> ...ps) {
        return t -> Arrays.stream(ps).reduce((a, b) -> new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return !a.test(t) && !b.test(t);
            }
        }).get().test(t);
    }
    public static <T> Predicate<T> any(Predicate<T> ...ps) {
        return t -> Arrays.stream(ps).reduce((a, b) -> new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return a.test(t) || b.test(t);
            }
        }).get().test(t);
    }

    public static <T> Predicate<T> all(Predicate<T> ...ps) {
        return t -> Arrays.stream(ps).reduce((a, b) -> new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return a.test(t) && b.test(t);
            }
        }).get().test(t);
    }

}
