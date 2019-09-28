package com.commercetools.sync.services;


import com.commercetools.sync.commons.models.WaitingToBeResolved;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface LazyResolutionService {

    /**
     * Given a custom object key, this method fetches a custom object that matches this given key in the CTP project
     * defined in a potentially injected {@link SphereClient}. If there is no matching custom object an empty
     * {@link Optional} will be returned in the returned future. A mapping of the key to the id of the fetched
     * custom object is persisted in an in -memory map.
     *
     * @param key the key of the custom object to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     * {@link Optional} that contains the matching {@link CustomObject} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Set<CustomObject<WaitingToBeResolved>>>
    fetch(@Nonnull final Set<String> keys);

    /**
     * Given a resource of type {@link CustomObject}, this method attempts to create a
     * {@link CustomObject} based on it in the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty option and the error callback will be triggered in those cases:
     * <ul>
     *     <li>the draft has a blank key</li>
     *     <li>the create request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets created/updated successfully on CTP, then the created resource
     * of {@link CompletionStage} in which the result of it's completion
     * contains an instance {@link Optional} of the resource which was created/updated.
     *
     * @param customObject the resource to create/update a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *     empty optional.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<WaitingToBeResolved>>> save(
        @Nonnull final WaitingToBeResolved productDraftWithUnresolvedRefs);

    /**
     * Given a resource of type {@link CustomObject}, this method attempts to delete a
     * {@link CustomObject} based on it in the CTP project defined by the sync options.
     *
     * <p>A completion stage containing an empty option and the error callback will be triggered in those cases:
     * <ul>
     *     <li>the draft has a blank key</li>
     *     <li>the delete request fails on CTP</li>
     * </ul>
     *
     * <p>On the other hand, if the resource gets deleted successfully on CTP, then the created resource
     * of {@link CompletionStage} in which the result of it's completion
     * contains an instance {@link Optional} of the resource which was deleted.
     *
     * @param customObject the resource to delete a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the deleted resource if successful otherwise an
     *     empty optional.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<WaitingToBeResolved>>> delete(
        @Nonnull final String key);
}