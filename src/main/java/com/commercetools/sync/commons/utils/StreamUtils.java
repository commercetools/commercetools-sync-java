package com.commercetools.sync.commons.utils;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public final class StreamUtils {

  /**
   * Applies the supplied {@code mapper} function on every non-null element in the supplied {@link
   * java.util.stream.Stream} of {@code elements}.
   *
   * @param elements the stream of elements.
   * @param mapper the mapper function to apply on every element.
   * @param <T> the type of the elements in the stream.
   * @param <S> the resulting type after applying the mapper function on an element.
   * @return a stream of the resulting mapped elements.
   */
  @Nonnull
  public static <T, S> Stream<S> filterNullAndMap(
      @Nonnull final Stream<T> elements, @Nonnull final Function<T, S> mapper) {

    return elements.filter(Objects::nonNull).map(mapper);
  }

  private StreamUtils() {}
}
