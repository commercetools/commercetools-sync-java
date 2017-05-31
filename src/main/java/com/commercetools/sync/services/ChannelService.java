package com.commercetools.sync.services;

import io.sphere.sdk.categories.Category;
import io.sphere.sdk.channels.Channel;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ChannelService {
    /**
     * Given a {@code key}, this method first checks if a cached map of channel keys -> ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all channels' keys to ids in
     * the CTP project, by querying the CTP project for all channels that contain the role {@code "InventorySupply"}.
     *
     * <p>After that, the method returns a {@link CompletionStage&lt;Optional&lt;String&gt;&gt;}
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Channel} was
     * found in the CTP project with this key.
     *
     * @param key the externalId by which a {@link Category} id should be fetched from the CTP project.
     * @return {@link CompletionStage&lt;Optional&lt;String&gt;&gt;} in which the result of it's completion could
     *      contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Channel} was
     *      found in the CTP project with this external id.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key);

    /**
     * Creates new supply channel of role {@code "InventorySupply"} and {@code key}.
     *
     * @param key key of supply channel
     * @return created {@link Channel}
     */
    @Nonnull
    CompletionStage<Channel> createChannel(@Nonnull final String key);

    /**
     * Creates new supply channel of role {@code "InventorySupply"} and {@code key} and puts a new mapping in
     * a cache map.
     *
     * @param key key of supply channel
     * @return created {@link Channel}
     */
    @Nonnull
    CompletionStage<Channel> createAndCacheChannel(@Nonnull final String key);

    /**
     * Adds a new mapping of key to id of the supplied channel to a cache map.
     *
     * @param channel key of supply channel
     */
    void cacheChannel(@Nonnull final Channel channel);
}
