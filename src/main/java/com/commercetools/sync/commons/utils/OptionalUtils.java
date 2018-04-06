package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public final class OptionalUtils {

    /**
     * Similar to Java 9
     * <a href="https://docs.oracle.com/javase/9/docs/api/java/util/Optional.html#stream--">Optional#stream()</a>:
     * converts Optional to a stream of one element if not empty. Otherwise - return an empty stream.
     *
     * @param optional Optional to convert
     * @param <T>      type of optional value
     * @return Stream of one element from the supplied {@code optional} if not empty, otherwise - an empty stream.
     */
    @Nonnull
    public static <T> Stream<T> filterEmptyOptionals(final @Nonnull Optional<T> optional) {
        return optional.map(Stream::of).orElseGet(Stream::empty);
    }


    private OptionalUtils() {
    }
}
