package com.commercetools.tests.utils;

import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class CompletionStageUtil {

  public static <T> T executeBlocking(@Nonnull final CompletionStage<T> request) {
    return request.toCompletableFuture().join();
  }

  private CompletionStageUtil() {}
}
