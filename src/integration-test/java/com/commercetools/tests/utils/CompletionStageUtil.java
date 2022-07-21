package com.commercetools.tests.utils;

import io.vrap.rmf.base.client.utils.ClientUtils;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;

public final class CompletionStageUtil {

  public static <T> T executeBlocking(@Nonnull final CompletionStage<T> request) {
    return ClientUtils.blockingWait(request.toCompletableFuture(), Duration.ofSeconds(10));
  }

  private CompletionStageUtil() {}
}
