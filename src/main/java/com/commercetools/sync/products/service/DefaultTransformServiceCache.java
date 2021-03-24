package com.commercetools.sync.products.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javax.annotation.Nonnull;

/**
 * This class is backed by caffeine cache with LRU cache eviction strategy, which is taking the size
 * for the caching needs.
 */
public class DefaultTransformServiceCache {

  public static final Cache<String, String> referenceIdToKeyCache = initializeCache();

  @Nonnull
  private static Cache<String, String> initializeCache() {
    return Caffeine.newBuilder().maximumSize(100_000).executor(Runnable::run).build();
  }
}
