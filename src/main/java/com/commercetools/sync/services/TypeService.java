package com.commercetools.sync.services;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface TypeService {

    /**
     * Filters out the keys which are already cached and fetches only the not-cached type keys from the CTP project
     * defined in an injected {@link SphereClient} and stores a mapping for every type to id in the cached map of
     * keys -&gt; ids and returns this cached map.
     *
     * @param typeKeys - a set type keys to fetch and cache the ids for
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a map of
     *      requested type keys -&gt; ids
     */
    @Nonnull
    CompletionStage<Map<String, String>> cacheKeysToIds(@Nonnull final Set<String> typeKeys);

    /**
     * Given a {@code key}, this method first checks if a cached map of Type keys -&gt; ids contains the key.
     * If not, it returns a completed future that contains an {@link Optional} that contains what this key maps to in
     * the cache. If the cache doesn't contain the key; this method attempts to fetch the id of the key from the CTP
     * project, caches it and returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the result of it's completion could contain an
     * {@link Optional} with the id inside of it or an empty {@link Optional} if no {@link Type} was
     * found in the CTP project with this key.
     *
     * @param key the key by which a {@link io.sphere.sdk.types.Type} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link Type} was found in the CTP project with this key.
     */
    @Nonnull
    CompletionStage<Optional<String>> fetchCachedTypeId(@Nonnull String key);

    /**
     * Given a {@link Set} of Type keys, this method fetches a set of all the Types, matching this given
     * set of keys in the CTP project, defined in an injected {@link io.sphere.sdk.client.SphereClient}. A
     * mapping of the key to the id of the fetched Type is persisted in an in-memory map.
     *
     * @param keys set of Type keys to fetch matching Type by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of its completion contains a {@link Set}
     *          of all matching Types.
     */
    @Nonnull
    CompletionStage<Set<Type>> fetchMatchingTypesByKeys(@Nonnull Set<String> keys);

    /**
     * Given a type key, this method fetches a type that matches this given key in the CTP project defined in an
     * injected {@link SphereClient}. If there is no matching type an empty {@link Optional} will be
     * returned in the returned future.
     *
     * @param key the key of the type to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of its completion contains an
     *         {@link Optional} that contains the matching {@link Type} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<Type>> fetchType(@Nullable String key);

    /**
     * Given a resource draft of type {@link TypeDraft}, this method attempts to create a resource
     * {@link Type} based on it in the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty optional and the error callback will be triggered in those cases:
     * <ul>
     *     <li>the draft has a blank key</li>
     *     <li>the create request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets created successfully on CTP, then the created resource's id and
     * key are cached and the method returns a {@link CompletionStage} in which the result of its completion
     * contains an instance {@link Optional} of the resource which was created.
     *
     * @param typeDraft the resource draft to create a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *         empty optional.
     */
    @Nonnull
    CompletionStage<Optional<Type>> createType(@Nonnull TypeDraft typeDraft);

    /**
     * Given a {@link Type} and a {@link List}&lt;{@link UpdateAction}&lt;{@link Type}&gt;&gt;, this method
     * issues an update request with these update actions on this {@link Type} in the CTP project defined in an
     * injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link Type}&gt; in which the result of its completion contains an instance of
     * the {@link Type} which was updated in the CTP project.
     *
     * @param type          the {@link Type} to update.
     * @param updateActions the update actions to update the {@link Type} with.
     * @return {@link CompletionStage}&lt;{@link Type}&gt; containing as a result of it's completion an instance of
     *         the {@link Type} which was updated in the CTP project or a {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Type> updateType(@Nonnull Type type,
                                     @Nonnull List<UpdateAction<Type>> updateActions);
}
