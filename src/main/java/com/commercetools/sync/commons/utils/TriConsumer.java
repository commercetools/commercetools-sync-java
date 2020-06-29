package com.commercetools.sync.commons.utils;

import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * Represents an operation that accepts three arguments and returns no result. This is the
 * three-arity specialization of {@link Consumer}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(Object, Object, Object)}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <V> the type of the third argument to the function
 * @see Consumer
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Performs operation on the given arguments.
     *
     * @param firstParam the first argument.
     * @param secondParam the second argument.
     * @param thirdParam the third argument.
     */
    void accept(@Nullable final T firstParam, @Nullable final U secondParam,
        @Nullable final V thirdParam);

}
