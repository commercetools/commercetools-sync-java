package com.commercetools.sync.services;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface CustomObjectService {

    /**
     * Given an {@code identifier}, this method first checks if {@code identifier#toString()}
     * is contained in a cached map of {@link CustomObjectCompositeIdentifier#toString()} -&gt; ids .
     * If it contains, it returns a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which String is what this identifier maps to in the cache. If the cache doesn't contain the identifier,
     * this method attempts to fetch the id of the identifier from the CTP project, caches it and returns
     * a {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt;
     * in which the {@link Optional} could contain the id inside of it.
     *
     * @param  identifier the identifier object containing CustomObject key and container, by which a
     *         {@link io.sphere.sdk.customobjects.CustomObject} id should be fetched from the CTP project.
     * @return {@link CompletionStage}&lt;{@link Optional}&lt;{@link String}&gt;&gt; in which the result of its
     *         completion could contain an {@link Optional} with the id inside of it or an empty {@link Optional} if no
     *         {@link CustomObject} was found in the CTP project with this identifier.
     */

    @Nonnull
    CompletionStage<Optional<String>> fetchCachedCustomObjectId(@Nonnull CustomObjectCompositeIdentifier identifier);

    /**
     * Given a {@link Set} of CustomObjectCompositeIdentifier, this method fetches a set of all the CustomObjects,
     * matching this given set of CustomObjectCompositeIdentifiers in the CTP project defined in an injected
     * {@link io.sphere.sdk.client.SphereClient}. A mapping of the CustomObjectCompositeIdentifier to the id of the
     * fetched CustomObject is persisted in an in-memory map.
     *
     * @param identifiers set of CustomObjectCompositeIdentifiers. Each identifier includes key and container to fetch
     *                    matching CustomObject.
     * @return {@link CompletionStage}&lt;{@link Map}&gt; in which the result of its completion contains a {@link Set}
     *          of all matching CustomObjects.
     */
    @Nonnull
    CompletionStage<Set<CustomObject<JsonNode>>> fetchMatchingCustomObjects(
        @Nonnull Set<CustomObjectCompositeIdentifier> identifiers);

    /**
     * Given a CustomObjectCompositeIdentifer identify which includes key and container of CustomObject, this method
     * fetches a CustomObject that matches this given identifier in the CTP project defined in an
     * injected {@link SphereClient}. If there is no matching CustomObject an empty {@link Optional} will be
     * returned in the returned future.
     *
     * @param identifier the identifier of the CustomObject to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of its completion contains an
     *         {@link Optional} that contains the matching {@link CustomObject} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<JsonNode>>> fetchCustomObject(
        @Nonnull CustomObjectCompositeIdentifier identifier);

    /**
     * Given a resource draft of CustomObject {@link CustomObjectDraft}, this method attempts to create or update
     * a resource {@link CustomObject} based on it in the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty optional and the error callback will be triggered in those cases:
     * <ul>
     *     <li>the draft has a blank key</li>
     *     <li>the create request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets created or updated successfully on CTP, then the created resource's
     * id and key/container wrapped by {@link CustomObjectCompositeIdentifier} are cached and the method returns a
     * {@link CompletionStage} in which the result of its completion contains an instance {@link Optional} of the
     * resource which was created or updated.
     *
     * <p>If an object with the given container/key exists on CTP, the object will be replaced with the new value and
     * the version is incremente.
     *
     * @param customObjectDraft the resource draft to create or update a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created/updated resource if successful
     *         otherwise an empty optional.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<JsonNode>>> upsertCustomObject(
        @Nonnull CustomObjectDraft<JsonNode> customObjectDraft);
}
