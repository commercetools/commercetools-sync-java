package com.commercetools.sync.services;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface CustomerGroupService {
    /**
     * If not already done once before, it fetches all the category keys from the CTP project defined in a potentially
     * injected {@link io.sphere.sdk.client.SphereClient} and stores a mapping for every customer group to id in
     * {@link Map} and returns this cached map.
     *
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of all
     * customer group keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds();

    /**
     * Given a {@code key}, this method first checks if a cached map of customer group keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all customer groups' keys to
     * ids in the CTP project, by querying the CTP project customer groups.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no
     * {@link io.sphere.sdk.customergroups.CustomerGroup} was found in the CTP project with this key.
     *
     * @param key the key by which a {@link io.sphere.sdk.customergroups.CustomerGroup} id should be fetched from the
     *            CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
     * completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     * {@link io.sphere.sdk.customergroups.CustomerGroup} was found in the CTP project with this external id.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedCustomerGroupId(@Nonnull final String key);
}
