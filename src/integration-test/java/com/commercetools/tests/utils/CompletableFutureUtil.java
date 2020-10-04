package com.commercetools.tests.utils;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionStage;

public final class CompletableFutureUtil {

    public static <T> T executeBlocking(@Nonnull final CompletionStage<T> request) {
        return request.toCompletableFuture().join();
    }

    private CompletableFutureUtil() {
    }
}
