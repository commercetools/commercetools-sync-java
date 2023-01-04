package com.commercetools.sync.sdk2.services.impl;

import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;

/** @param <S> Subclass of {@link BaseSyncOptions} */
abstract class BaseService<S extends BaseSyncOptions> {

  final S syncOptions;
  protected final Cache<String, String> keyToIdCache;

  protected static final int MAXIMUM_ALLOWED_UPDATE_ACTIONS = 500;
  static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";

  /*
   * To be more practical, considering 41 characters as an average for key and sku fields
   * (key and sku field doesn't have limit except for ProductType(256)) We chunk them in 250
   * (keys or sku) we will have a query around 11.000 characters(also considered some
   * conservative space for headers). Above this size it could return - Error 414 (Request-URI Too Large)
   */
  static final int CHUNK_SIZE = 250;

  BaseService(@Nonnull final S syncOptions) {
    this.syncOptions = syncOptions;
    this.keyToIdCache =
        Caffeine.newBuilder()
            .maximumSize(syncOptions.getCacheSize())
            .executor(Runnable::run)
            .build();
  }

  /**
   * Given a set of keys this method collects all keys which aren't already contained in the cache
   * {@code keyToIdCache}
   *
   * @param keys {@link Set} of keys
   * @return a {@link Set} of keys which aren't already contained in the cache or empty
   */
  @Nonnull
  protected Set<String> getKeysNotCached(@Nonnull final Set<String> keys) {
    return keys.stream()
        .filter(StringUtils::isNotBlank)
        .filter(key -> !keyToIdCache.asMap().containsKey(key))
        .collect(Collectors.toSet());
  }
}
