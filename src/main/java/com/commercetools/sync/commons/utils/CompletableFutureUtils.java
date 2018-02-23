package com.commercetools.sync.commons.utils;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class CompletableFutureUtils {

    private static final CompletableFuture<Optional<?>> EMPTY = CompletableFuture.completedFuture(Optional.empty());

    private CompletableFutureUtils() { }

    /**
     * Returns a {@link CompletableFuture} of an empty {@link Optional}. There is no guarantee
     * that it is a singleton.
     * @return {@link CompletableFuture} of an empty {@link Optional}
     * */
    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> emptyOptionalCompletedFuture() {
        return (CompletableFuture<T>)EMPTY;
    }
}
