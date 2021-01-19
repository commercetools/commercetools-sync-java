package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customergroups.CustomerGroup;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CustomerGroupService {

  /**
   * Filters out the keys which are already cached and fetches only the not-cached customer group
   * keys from the CTP project defined in an injected {@link SphereClient} and stores a mapping for
   * every customer group to id in the cached map of keys -&gt; ids and returns this cached map.
   *
   * @param customerGroupKeys - a set customer group keys to fetch and cache the ids for
   * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion
   *     contains a map of requested customer group keys -&gt; ids
   */
  @Nonnull
  CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> customerGroupKeys);

  /**
   * Given a {@code key}, this method first checks if a cached map of customer group keys -&gt; ids
   * contains the key. If not, it returns a completed future that contains an {@link Optional} that
   * contains what this key maps to in the cache. If the cache doesn't contain the key; this method
   * attempts to fetch the id of the key from the CTP project, caches it and returns a {@link
   * CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
   * completion could contain an {@link Optional} with the id inside of it or an empty {@link
   * Optional} if no {@link CustomerGroup} was found in the CTP project with this key.
   *
   * @param key the key by which a {@link CustomerGroup} id should be fetched from the CTP project.
   * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the
   *     result of its completion could contain an {@link Optional} with the id inside of it or an
   *     empty {@link Optional} if no {@link CustomerGroup} was found in the CTP project with this
   *     key or a blank (null/empty string) key was supplied.
   */
  @Nonnull
  CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nullable String key);
}
