package com.commercetools.sync.commons.utils;

import javax.annotation.Nullable;

@FunctionalInterface
public interface TriFunction<T,U,S, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param firstParam the first parameter.
     * @param secondParam the second parameter.
     * @param thirdParam the third parameter.
     * @return the function result.
     */
    R apply(@Nullable final T firstParam, @Nullable final U secondParam, @Nullable final S thirdParam);
}
