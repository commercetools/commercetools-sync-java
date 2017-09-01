package com.commercetools.sync.services;

import io.sphere.sdk.channels.Channel;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ChannelService {
    /**
     * Given a {@code key}, this method first checks if a cached map of channel keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all channels' keys to ids in
     * the CTP project, by querying the CTP project channels.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Channel} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link Channel} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of it's
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link Channel} was found in the CTP project with this external id.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedChannelId(@Nonnull final String key);

    /**
     * Creates a new channel with the supplied {@code key}.
     *
     * @param key   key of supply channel.
     * @return a future containing as a result the created {@link Channel} or a sphere exception.
     */
    @Nonnull
    CompletionStage<Channel> createChannel(@Nonnull final String key);

    /**
     * Creates a new channel with the supplied {@code key} and puts a new mapping of it's key
     * to id in a cache map.
     *
     * @param key key of supply channel.
     * @return a future containing as a result the created {@link Channel} or a sphere exception.
     */
    @Nonnull
    CompletionStage<Channel> createAndCacheChannel(@Nonnull final String key);

    /**
     * Adds a new mapping of key to id of the supplied channel to a cache map.
     *
     * @param channel key of supply channel
     */
    void cacheChannel(@Nonnull final Channel channel);

    /**
     * Invalidates the state of the cache so that on the next fetch a fresh copy is fetched and cached.
     */
    void invalidateCache();
}
