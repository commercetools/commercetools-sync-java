package com.commercetools.sync.commons.utils;

import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Represents an operation that accepts four arguments and returns no result. This is the quad-arity
 * specialization of {@link Consumer}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a> whose functional method is
 * {@link #accept(Object, Object, Object, Object)}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <S> the type of the third argument to the function
 * @param <V> the type of the fourth argument to the function
 * @see Consumer
 */
@FunctionalInterface
public interface QuadConsumer<T, U, S, V> {

  /**
   * Performs operation on the given arguments.
   *
   * @param firstParam the first argument.
   * @param secondParam the second argument.
   * @param thirdParam the third argument.
   * @param fourthParam the fourth argument.
   */
  void accept(
      @Nullable final T firstParam,
      @Nullable final U secondParam,
      @Nullable final S thirdParam,
      @Nullable final V fourthParam);
}
