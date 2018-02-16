package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.CompletableFuture.allOf;

public final class CompletableFutureUtils {

    /**
     * Creates a Future containing a list of value results after the mapper function is applied to each one and
     * completed.
     *
     * @param values list of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @return a future containing a list of completed stage results of the values after the mapper function was
     *         applied to each one.
     */
    public static <T> CompletableFuture<List<T>> mapValuesToFutureOfCompletedValues(
        @Nonnull final List<T> values,
        @Nonnull final Function<T, CompletionStage<T>> mapper) {
        return mapValuesToFutureOfCompletedValues(values.stream(), mapper);
    }

    /**
     * Creates a Future containing a list of value results after the mapper function is applied to each one and
     * completed.
     *
     * @param values stream of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @return a future containing a list of completed stage results of the values after the mapper function was
     *         applied to each one.
     */
    public static <T> CompletableFuture<List<T>> mapValuesToFutureOfCompletedValues(
        @Nonnull final Stream<T> values,
        @Nonnull final Function<T, CompletionStage<T>> mapper) {
        return getFutureOfCompletedFutures(mapValuesToFutures(values, mapper));
    }

    /**
     * Completes the supplied list of futures in parallel then returns a future containing a list of the completed
     * results.
     *
     * @param futures list of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param <T>     The type of the future results.
     * @return a future containing a list of the completed results.
     */
    public static <T> CompletableFuture<List<T>> getFutureOfCompletedFutures(
        @Nonnull final List<CompletableFuture<T>> futures) {

        return allOf(futures.toArray(new CompletableFuture[futures.size()]))
            .thenApply(result -> futures.stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.toList()));
    }

    /**
     * Maps a stream of values to futures using the supplied mapper function.
     *
     * @param values stream of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @return a list of futures resulting from applying the mapper function on each value.
     */
    public static <T> List<CompletableFuture<T>> mapValuesToFutures(
        @Nonnull final Stream<T> values,
        @Nonnull final Function<T, CompletionStage<T>> mapper) {

        return values.filter(Objects::nonNull)
                     .map(mapper)
                     .map(CompletionStage::toCompletableFuture)
                     .collect(Collectors.toList());
    }


    private CompletableFutureUtils() {
    }
}
