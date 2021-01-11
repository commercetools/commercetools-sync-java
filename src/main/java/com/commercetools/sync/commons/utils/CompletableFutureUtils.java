package com.commercetools.sync.commons.utils;

import static com.commercetools.sync.commons.utils.StreamUtils.filterNullAndMap;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public final class CompletableFutureUtils {

  /**
   * Creates a Future containing a collection of value results after the mapper function is applied
   * to each value in the supplied collection and completed it. The type of the returned collection
   * is decided by the supplied collector.
   *
   * @param values collection of values to apply a mapper function that would map each to a {@link
   *     CompletionStage}.
   * @param mapper function to map each value to a {@link CompletionStage}
   * @param collector the collector to define the type of the collection returned.
   * @param <T> The type of the values.
   * @param <S> The type of the mapped completed values.
   * @param <U> The type of the collection returned in the future.
   * @return a future containing a collection of completed stage results of the values after the
   *     mapper function was applied to each value in the supplied collection.
   */
  @Nonnull
  public static <T, S, U extends Collection<S>>
      CompletableFuture<U> mapValuesToFutureOfCompletedValues(
          @Nonnull final Collection<T> values,
          @Nonnull final Function<T, CompletionStage<S>> mapper,
          @Nonnull final Collector<S, ?, U> collector) {

    return mapValuesToFutureOfCompletedValues(values.stream(), mapper, collector);
  }

  /**
   * Creates a Future containing a stream of value results after the mapper function is applied to
   * each value in the supplied collection and completed it. The type of the collection is decided
   * upon on by the supplied collector.
   *
   * @param values collection of values to apply a mapper function that would map each to a {@link
   *     CompletionStage}.
   * @param mapper function to map each value to a {@link CompletionStage}
   * @param <T> The type of the values.
   * @param <S> The type of the mapped completed values.
   * @return a future containing a stream of completed stage results of the values after the mapper
   *     function was applied to each value in the supplied collection.
   */
  @Nonnull
  public static <T, S> CompletableFuture<Stream<S>> mapValuesToFutureOfCompletedValues(
      @Nonnull final Collection<T> values, @Nonnull final Function<T, CompletionStage<S>> mapper) {

    final List<CompletableFuture<S>> futureList =
        mapValuesToFutures(values.stream(), mapper, toList());
    return collectionOfFuturesToFutureOfCollection(futureList, toList()).thenApply(List::stream);
  }

  /**
   * Creates a Future containing a collection of value results after the mapper function is applied
   * to each value in the supplied stream and completed it. The type of the returned collection is
   * decided by the supplied collector.
   *
   * @param values stream of values to apply a mapper function that would map each to a {@link
   *     CompletionStage}.
   * @param mapper function to map each value to a {@link CompletionStage}
   * @param collector the collector to define the type of the collection returned.
   * @param <T> The type of the values.
   * @param <S> The type of the mapping of the values.
   * @param <U> The type of the collection returned in the future.
   * @return a future containing a list of completed stage results of the values after the mapper
   *     function was applied to each one.
   */
  @Nonnull
  public static <T, S, U extends Collection<S>>
      CompletableFuture<U> mapValuesToFutureOfCompletedValues(
          @Nonnull final Stream<T> values,
          @Nonnull final Function<T, CompletionStage<S>> mapper,
          @Nonnull final Collector<S, ?, U> collector) {

    final List<CompletableFuture<S>> futureList = mapValuesToFutures(values, mapper, toList());
    return collectionOfFuturesToFutureOfCollection(futureList, collector);
  }

  /**
   * Transforms a collection of {@code CompletionStage} into a {@code CompletionStage} of a
   * collection, that will be completed once all the elements of the given futures are completed. In
   * case multiple stages end exceptionally only one error is kept. The type of the returned
   * collection is decided by the supplied collector.
   *
   * <p>Note: Null futures in the collection are filtered out.
   *
   * @param futures collection of {@code CompletionStage}
   * @param collector the collector to define the type of the collection returned.
   * @param <T> the element obtained from the set of {@code CompletionStage}
   * @param <S> The type of the collection returned in the future.
   * @return the {@code CompletableFuture} of a collection of elements
   */
  @Nonnull
  public static <T, S extends Collection<T>>
      CompletableFuture<S> collectionOfFuturesToFutureOfCollection(
          @Nonnull final Collection<? extends CompletionStage<T>> futures,
          @Nonnull final Collector<T, ?, S> collector) {

    final List<CompletableFuture<T>> futureList =
        futures.stream()
            .filter(Objects::nonNull)
            .map(CompletionStage::toCompletableFuture)
            .collect(toList());

    final CompletableFuture[] futuresAsArray =
        futureList.toArray(new CompletableFuture[futureList.size()]);
    return CompletableFuture.allOf(futuresAsArray)
        .thenApply(
            ignoredResult -> futureList.stream().map(CompletableFuture::join).collect(collector));
  }

  /**
   * Maps a stream of values to a future collection using the supplied mapper function. The type of
   * the returned collection is decided by the supplied collector.
   *
   * @param values stream of values to apply a mapper function that would map each to a {@link
   *     CompletionStage}.
   * @param mapper function to map each value to a {@link CompletionStage}
   * @param collector the collector to define the type of the collection returned.
   * @param <T> The type of the values.
   * @param <S> The type of the mapped values.
   * @param <U> The type of the collection returned.
   * @return a collection of futures resulting from applying the mapper function on each value.
   */
  @Nonnull
  public static <T, S, U extends Collection<CompletableFuture<S>>> U mapValuesToFutures(
      @Nonnull final Stream<T> values,
      @Nonnull final Function<T, CompletionStage<S>> mapper,
      @Nonnull final Collector<CompletableFuture<S>, ?, U> collector) {

    final Stream<CompletionStage<S>> stageStream = filterNullAndMap(values, mapper);
    return toCompletableFutures(stageStream, collector);
  }

  /**
   * Converts a stream of {@link CompletionStage} of values of type {@code <T>} into a {@link
   * Collection} of the type of the supplied {@code collector} of {@link CompletableFuture} of
   * values of type {@code <T>}. The type of the returned collection is decided by the supplied
   * collector.
   *
   * <p>Note: Null futures in the stream are filtered out.
   *
   * @param values stream of {@link CompletionStage} of values of type {@code <T>}
   * @param collector the collector to define the type of the collection returned.
   * @param <T> the type of the results of the stages.
   * @param <S> the concrete type of the collection returned.
   * @return a {@link List} of {@link CompletableFuture} elements of type {@code <T>}.
   */
  @Nonnull
  public static <T, S extends Collection<CompletableFuture<T>>> S toCompletableFutures(
      @Nonnull final Stream<CompletionStage<T>> values,
      @Nonnull final Collector<CompletableFuture<T>, ?, S> collector) {
    return values
        .filter(Objects::nonNull)
        .map(CompletionStage::toCompletableFuture)
        .collect(collector);
  }

  private CompletableFutureUtils() {}
}
