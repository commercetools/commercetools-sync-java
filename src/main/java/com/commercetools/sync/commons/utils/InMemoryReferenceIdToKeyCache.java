package com.commercetools.sync.commons.utils;

import com.commercetools.sync.products.service.ProductReferenceTransformService;
import com.commercetools.sync.services.impl.BaseTransformServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.Map;

/**
 * A thread safe hash map to store reference id to key pairs.
 *
 * <p>Designed to be used as an reference in memory cache as a part of {@link
 * BaseTransformServiceImpl} instances, such as {@link ProductReferenceTransformService}.
 *
 * <p>The map is implemented by the caffeine library which implements a LRU based cache eviction
 * strategy. It means unused id to key pairs will be evicted, also it stores max 10000 pairs to
 * ensure the minimum memory consumption.
 */
public final class InMemoryReferenceIdToKeyCache {
  private static final Cache<String, String> referenceIdToKeyCache =
      Caffeine.newBuilder().maximumSize(10_000).executor(Runnable::run).build();

  /**
   * Note that modifications made to the map directly affect the cache.
   *
   * @return a view of the entries stored in this cache as a thread-safe map.
   */
  public static Map<String, String> getInstance() {
    return referenceIdToKeyCache.asMap();
  }

  private InMemoryReferenceIdToKeyCache() {}
}
