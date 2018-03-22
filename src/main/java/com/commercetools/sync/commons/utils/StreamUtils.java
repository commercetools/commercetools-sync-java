package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public final class StreamUtils {

    /**
     * Applies the supplied {@code mapper} function on every non null element in the supplied {@link Stream} of
     * {@code elements}.
     *
     * @param elements the stream of elements.
     * @param mapper   the mapper function to apply on every element.
     * @param <T>      the type of the elements in the stream.
     * @param <S>      the resulting type after applying the mapper function on an element.
     * @return a stream of the resulting mapped elements.
     */
    @Nonnull
    public static <T, S> Stream<S> filterNullAndMap(
        @Nonnull final Stream<T> elements,
        @Nonnull final Function<T, S> mapper) {

        return elements.filter(Objects::nonNull).map(mapper);
    }

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

    /**
     * Similar to {@link #filterEmptyOptionals(Optional)}, but returns a lambda with the same behavior.
     *
     * @param <T> type of optional value
     * @return function which returns a stream of one element from the supplied {@code optional} if not empty,
     *     otherwise - an empty stream.
     */
    @Nonnull
    public static <T> Function<Optional<? extends T>, Stream<? extends T>> getFilterEmptyOptionals() {
        return optional -> optional.map(Stream::of).orElseGet(Stream::empty);
    }

    private StreamUtils() {
    }
}
