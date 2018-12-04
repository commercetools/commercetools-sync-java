package com.commercetools.sync.services;

import io.sphere.sdk.channels.Channel;
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
     * Given a {@link Set} of Type keys, this method fetches a set of all the ProductTypes, matching this given
     * set of keys in the CTP project, defined in a potentially injected {@link io.sphere.sdk.client.SphereClient}. A
     * mapping of the key to the id of the fetched Type is persisted in an in-memory map.
     *
     * @param keys set of Type keys to fetch matching Type by.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of it's completion contains a {@link Set}
     *          of all matching Type.
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
     * Given a {@link TypeDraft}, this method creates a {@link Type} based on it in the CTP project defined in
     * a potentially injected {@link io.sphere.sdk.client.SphereClient}. The created type's id and key are also
     * cached. This method returns {@link CompletionStage}&lt;{@link Type}&gt; in which the result of it's
     * completion contains an instance of the {@link Type} which was created in the CTP project.
     *
     * @param typeDraft the {@link TypeDraft} to create a {@link Type} based off of.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     *         {@link Optional} that contains the matching {@link Type} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<Type>> createType(@Nonnull final TypeDraft typeDraft);

    /**
     * Given a {@link Type} and a {@link List}&lt;{@link UpdateAction}&lt;{@link Type}&gt;&gt;, this method
     * issues an update request with these update actions on this {@link Type} in the CTP project defined in a
     * potentially injected {@link io.sphere.sdk.client.SphereClient}. This method returns
     * {@link CompletionStage}&lt;{@link Type}&gt; in which the result of it's completion contains an instance of
     * the {@link Type} which was updated in the CTP project.
     *
     * @param type          the {@link Type} to update.
     * @param updateActions the update actions to update the {@link Type} with.
     * @return {@link CompletionStage}&lt;{@link Type}&gt; containing as a result of it's completion an instance of
     *         the {@link Type} which was updated in the CTP project or a {@link io.sphere.sdk.models.SphereException}.
     */
    @Nonnull
    CompletionStage<Type> updateType(@Nonnull final Type type,
                                     @Nonnull final List<UpdateAction<Type>> updateActions);
}
