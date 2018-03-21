package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
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
     * Resolves all not empty optionals of a given {@link Stream} and returns them as a {@link List}.
     *
     * @param elements the stream of elements.
     * @return a {@link List} of resolved optionals.
     */
    @Nonnull
    public static <T extends List<?>> T asList(@Nonnull final Stream<? extends Optional> elements) {
        return filterStream(elements, Collectors.toList());
    }

    /**
     * Resolves all not empty optionals of a given {@link Stream} and returns them as a {@link Set}.
     *
     * @param elements the stream of elements.
     * @return a {@link Set} of resolved optionals.,
     */
    @Nonnull
    public static <T extends Set<?>> T asSet(@Nonnull final Stream<? extends Optional> elements) {
        return filterStream(elements, Collectors.toSet());
    }

    /**
     * Resolves all not empty optionals of a given {@link Stream} and returns them as a Collection<T>.
     *
     * @param elements the stream of elements.
     * @param  {@link Collector}, that accumulates input elements into a mutable result container
     * @return a {@link Collection<T>} of resolved optionals.
     */
    @Nonnull
    private static <T extends Collection> T filterStream(@Nonnull final Stream<? extends Optional> elements, @Nonnull
        Collector<Object, ?, ?> collector) {
        return (T) elements.filter(Optional::isPresent).map(Optional::get).collect(collector);
    }

    private StreamUtils() {
    }
}
