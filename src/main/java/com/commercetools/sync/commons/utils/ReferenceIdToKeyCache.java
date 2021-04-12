package com.commercetools.sync.commons.utils;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * A Interface used to manage cache to store reference id to key pairs.
 *
 * <p>The cache can be implemented by any caching library and override the methods to perform
 * caching operations. Default cache implementation is provided in the library class
 * CaffeineReferenceIdToKeyCacheImpl{@link CaffeineReferenceIdToKeyCacheImpl}.
 */
public interface ReferenceIdToKeyCache {

  /**
   * @param key key with which the specified value is to be associated.
   * @param value value to be associated with the specified key.
   */
  void add(@Nonnull final String key, @Nonnull final String value);

  /** @param key key whose mapping is to be removed from the map */
  void remove(@Nonnull final String key);

  /** @param idToKeyValues mappings to be stored in this cache. */
  void addAll(@Nonnull final Map<String, String> idToKeyValues);

  /**
   * @param key key whose presence in this map is to be tested
   * @return true if this map contains a mapping for the specified key in the cache.
   */
  boolean containsKey(@Nonnull final String key);

  /**
   * @param key the key whose associated value is to be returned.
   * @return the value to which the specified key is mapped, or {@code null} if the cache contains
   *     no mapping for the key.
   */
  String get(@Nonnull final String key);

  /** Discards all entries in the cache. */
  void clearCache();
}
