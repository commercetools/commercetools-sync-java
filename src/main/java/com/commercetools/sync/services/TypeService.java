package com.commercetools.sync.services;


import io.sphere.sdk.channels.Channel;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface TypeService {
    /**
     * Given a {@code key}, this method first checks if a cached map of Type keys -> ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all Type keys to ids in
     * the CTP project, by querying the CTP project for all Types.
     *
     * <p>After that, the method returns a {@link CompletionStage&lt;Optional&lt;String&gt;&gt;}
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Channel} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link io.sphere.sdk.types.Type} id should be fetched from the CTP project.
     * @return {@link CompletionStage&lt;Optional&lt;String&gt;&gt;} in which the result of it's completion could
     *      contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Channel} was
     *      found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull final String key);
}
