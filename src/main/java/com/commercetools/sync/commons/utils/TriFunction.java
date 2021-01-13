package com.commercetools.sync.commons.utils;

import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Represents a function that accepts three arguments and produces a result. This is the three-arity
 * specialization of {@link Function}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a> whose functional method is
 * {@link #apply(Object, Object, Object)}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <S> the type of the third argument to the function
 * @param <R> the type of the result of the function
 * @see Function
 */
@FunctionalInterface
public interface TriFunction<T, U, S, R> {

  /**
   * Applies this function to the given arguments.
   *
   * @param firstParam the first argument.
   * @param secondParam the second argument.
   * @param thirdParam the third argument.
   * @return the function result.
   */
  R apply(@Nullable T firstParam, @Nullable U secondParam, @Nullable S thirdParam);
}
