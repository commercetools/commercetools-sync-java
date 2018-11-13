package com.commercetools.sync.services;


import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface TypeService {
    /**
     * Given a {@code key}, this method first checks if a cached map of Type keys -&gt; ids is not empty.
     * If not, it returns a completed future that contains an optional that contains what this key maps to in
     * the cache. If the cache is empty, the method populates the cache with the mapping of all Type keys to ids in
     * the CTP project, by querying the CTP project for all Types.
     *
     * <p>After that, the method returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Channel} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link io.sphere.sdk.types.Type} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link Channel} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull final String key);

    /**
     * Queries existing {@link Type}'s against set of keys.
     *
     * @param keys {@link Set} of sku values, used in search predicate
     * @return {@link CompletionStage} of matching types or empty set when there is no type with corresponding
     * {@code keys}.
     */
    @Nonnull
    CompletionStage<Set<Type>> fetchMatchingTypesByKeys(@Nonnull final Set<String> keys);

    /**
     * Given a type key, this method fetches a type that matches this given key in the CTP project defined in a
     * potentially injected {@link SphereClient}. If there is no matching type an empty {@link Optional} will be
     * returned in the returned future.
     *
     * @param key the key of the type to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@link Type} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<Type>> fetchType(@Nullable final String key);

    /**
     * Creates new type from {@code typeDraft}.
     *
     * @param typeDraft draft with data for new type
     * @return {@link CompletionStage} with created {@link Type}.
     */
    @Nonnull
    CompletionStage<Type> createType(@Nonnull final TypeDraft typeDraft);

    /**
     * Updates existing type with {@code updateActions}.
     *
     * @param type  type that should be updated
     * @param updateActions {@link List} of actions that should be applied to {@code type}
     * @return {@link CompletionStage} with updated {@link Type}.
     */
    @Nonnull
    CompletionStage<Type> updateType(@Nonnull final Type type,
                                     @Nonnull final List<UpdateAction<Type>> updateActions);
}
