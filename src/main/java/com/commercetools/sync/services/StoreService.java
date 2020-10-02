package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.stores.Store;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface StoreService {

    /**
     * Filters out the keys which are already cached and fetches only the not-cached store keys from the CTP
     * project defined in an injected {@link SphereClient} and stores a mapping for every store to id in
     * the cached map of keys -&gt; ids and returns this cached map.
     *
     * @param storeKeys - a set store keys to fetch and cache the ids for
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of
     *      requested store keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> storeKeys);

    /**
     * Given a {@code key}, this method first checks if a cached map of store keys -&gt; ids contains the key.
     * If not, it returns a completed future that contains an {@link Optional} that contains what this key maps to in
     * the cache. If the cache doesn't contain the key; this method attempts to fetch the id of the key from the CTP
     * project, caches it and returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of its completion could contain an {@link Optional} with the id inside of it or an empty
     * {@link Optional} if no {@link Store} was found in the CTP project with this
     * key.
     *
     * @param key the key by which a {@link Store} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link Store} was found in the CTP project with this key or a blank (null/empty string) key was
     *         supplied.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedStoreId(@Nullable String key);
}
