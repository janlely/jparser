package org.jay.parser.util;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * useful fucntions
 */
public class F {

    /**
     * @param predicate the predicate
     * @param <T> the type
     * @return A new predicate
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return t -> !predicate.test(t);
    }

    /**
     * @param ps  predicates
     * @param <T> the type
     * @return A new predicate
     */
    public static <T> Predicate<T> noneOf(Predicate<T> ...ps) {
        return t -> Arrays.stream(ps).reduce((a, b) -> new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return !a.test(t) && !b.test(t);
            }
        }).get().test(t);
    }

    /**
     * @param ps predicates
     * @param <T> the type
     * @return A new predicate
     */
    public static <T> Predicate<T> any(Predicate<T> ...ps) {
        return t -> Arrays.stream(ps).reduce((a, b) -> new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return a.test(t) || b.test(t);
            }
        }).get().test(t);
    }

    /**
     * @param ps predicates
     * @param <T> the type
     * @return A new predicate
     */
    public static <T> Predicate<T> all(Predicate<T> ...ps) {
        return t -> Arrays.stream(ps).reduce((a, b) -> new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return a.test(t) && b.test(t);
            }
        }).get().test(t);
    }

}
