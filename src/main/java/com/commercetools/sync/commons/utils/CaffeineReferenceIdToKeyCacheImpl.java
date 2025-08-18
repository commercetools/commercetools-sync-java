package com.commercetools.sync.commons.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;

/**
 * A Caffeine cache to store reference id to key pairs.
 *
 * <p>Designed to be used as an reference in memory cache as a part of {@link
 * com.commercetools.sync.services.impl.BaseTransformServiceImpl} instances.
 */
public class CaffeineReferenceIdToKeyCacheImpl implements ReferenceIdToKeyCache {
  private static final Cache<String, String> referenceIdToKeyCache =
      Caffeine.newBuilder().executor(Runnable::run).build();

  public CaffeineReferenceIdToKeyCacheImpl() {}

  @Override
  public void add(String key, String value) {
    referenceIdToKeyCache.put(key, value);
  }

  @Override
  public void remove(String key) {
    referenceIdToKeyCache.invalidate(key);
  }

  @Override
  public void addAll(Map<String, String> idToKeyValues) {
    referenceIdToKeyCache.putAll(idToKeyValues);
  }

  @Override
  public boolean containsKey(String key) {
    return (null != referenceIdToKeyCache.getIfPresent(key));
  }

  @Override
  public String get(String key) {
    return referenceIdToKeyCache.getIfPresent(key);
  }

  @Override
  public void clearCache() {
    referenceIdToKeyCache.invalidateAll();
  }
}
