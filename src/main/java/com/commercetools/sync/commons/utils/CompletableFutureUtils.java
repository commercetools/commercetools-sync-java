package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.commercetools.sync.commons.utils.StreamUtils.map;
import static io.sphere.sdk.utils.CompletableFutureUtils.listOfFuturesToFutureOfList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

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
    public static <T> CompletableFuture<List<T>> mapValuesListToFutureOfCompletedSameTypes(
        @Nonnull final List<T> values,
        @Nonnull final Function<T, CompletionStage<T>> mapper) {
        return mapValuesToFutureOfCompletedSameTypes(values.stream(), mapper);
    }

    /**
     * Creates a Future containing a list of value results after the mapper function is applied to each one and
     * completed.
     *
     * @param values stream of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @param <S>    The type of the mapping of the values.
     * @return a future containing a list of completed stage results of the values after the mapper function was
     *         applied to each one.
     */
    public static <T, S> CompletableFuture<Stream<S>> mapValuesSetToFutureOfCompletedDifferentTypes(
        @Nonnull final Set<T> values,
        @Nonnull final Function<T, CompletionStage<S>> mapper) {
        return setOfFuturesToFutureOfSet(mapValueSetToFuturesOfDifferentType(values, mapper))
            .thenApply(Set::stream);
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
    public static <T> CompletableFuture<List<T>> mapValuesToFutureOfCompletedSameTypes(
        @Nonnull final Stream<T> values,
        @Nonnull final Function<T, CompletionStage<T>> mapper) {
        return listOfFuturesToFutureOfList(mapValuesToFuturesOfSameType(values, mapper));
    }

    /**
     * Transforms a set of {@code CompletionStage} into a {@code CompletionStage} of a set,
     * that will be completed once all the elements of the given set are completed.
     * In case multiple stages end exceptionally only one error is kept.
     *
     * @param set set of {@code CompletionStage}
     * @param <T> the element obtained from the set of {@code CompletionStage}
     * @return the {@code CompletableFuture} of a set of elements
     */
    public static <T> CompletableFuture<Set<T>> setOfFuturesToFutureOfSet(
        final Set<? extends CompletionStage<T>> set) {
        final Set<CompletableFuture<T>> futureList = set.stream()
                                                        .map(CompletionStage::toCompletableFuture)
                                                        .collect(toSet());

        final CompletableFuture[] futuresAsArray = futureList.toArray(new CompletableFuture[futureList.size()]);
        return CompletableFuture.allOf(futuresAsArray)
                                .thenApply(ignoredResult -> futureList.stream()
                                                                      .map(CompletableFuture::join)
                                                                      .collect(Collectors.toSet()));
    }

    /**
     * Maps a stream of values to futures (containing results of the same type) using the supplied mapper function.
     *
     * @param values stream of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @return a list of futures resulting from applying the mapper function on each value.
     */
    public static <T> List<CompletableFuture<T>> mapValuesToFuturesOfSameType(
        @Nonnull final Stream<T> values,
        @Nonnull final Function<T, CompletionStage<T>> mapper) {

        return toListOfCompletableFutures(map(values, mapper));
    }

    /**
     * Maps a stream of values to futures (containing results of a different type) using the supplied mapper function.
     *
     * @param values stream of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @param <S>    The type of the mapped values.
     * @return a list of futures resulting from applying the mapper function on each value.
     */
    public static <T, S> List<CompletableFuture<S>> mapValuesToFuturesOfDifferentType(
        @Nonnull final Stream<T> values,
        @Nonnull final Function<T, CompletionStage<S>> mapper) {

        return toListOfCompletableFutures(map(values, mapper));
    }

    /**
     * Maps a stream of values to futures (containing results of a different type) using the supplied mapper function.
     *
     * @param values stream of values to apply a mapper function that would map each to a {@link CompletionStage}.
     * @param mapper function to map each value to a {@link CompletionStage}
     * @param <T>    The type of the values.
     * @param <S>    The type of the mapped values.
     * @return a list of futures resulting from applying the mapper function on each value.
     */
    public static <T, S> Set<CompletableFuture<S>> mapValueSetToFuturesOfDifferentType(
        @Nonnull final Set<T> values,
        @Nonnull final Function<T, CompletionStage<S>> mapper) {

        return toSetOfCompletableFutures(map(values.stream(), mapper));
    }


    /**
     * Converts a stream of {@link CompletionStage} of values of type {@code <T>} into a
     * {@link List}{@link CompletableFuture} of values of type {@code <T>}.
     *
     * @param values stream of {@link CompletionStage} of values of type {@code <T>}
     * @param <T>    the type of the results of the stages.
     * @return a {@link List} of {@link CompletableFuture} elements of type {@code <T>}.
     */
    public static <T> List<CompletableFuture<T>> toListOfCompletableFutures(
        @Nonnull final Stream<CompletionStage<T>> values) {

        return values.map(CompletionStage::toCompletableFuture).collect(toList());
    }

    /**
     * Converts a stream of {@link CompletionStage}&lt;T&gt; into a
     * {@link Set}&lt;{@link CompletableFuture}&lt;T&gt;&gt;.
     *
     * @param values stream of {@link CompletionStage} of type {@code <T>}
     * @param <T>    the type of the results of the stages.
     * @return a {@link Set} of {@link CompletableFuture}&lt;T&gt;.
     */
    public static <T> Set<CompletableFuture<T>> toSetOfCompletableFutures(
        @Nonnull final Stream<CompletionStage<T>> values) {

        return values.map(CompletionStage::toCompletableFuture).collect(toSet());
    }



    private CompletableFutureUtils() {
    }
}
