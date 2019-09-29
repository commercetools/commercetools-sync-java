package com.commercetools.sync.services;


import com.commercetools.sync.commons.models.NonResolvedReferencesCustomObject;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface LazyResolutionService {

    /**
     * Given a custom object key, this method fetches a custom object that matches this given key in the CTP project
     * defined in a potentially injected {@link SphereClient}. If there is no matching custom object an empty
     * {@link Optional} will be returned in the returned future.
     *
     * @param key the key of the custom object to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     * {@link Optional} that contains the matching {@link CustomObject} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>> fetch(
            @Nullable final String key);

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
    CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>> createOrUpdateCustomObject(
            @Nonnull final CustomObjectDraft<NonResolvedReferencesCustomObject> customObject);

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
    CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>> deleteCustomObject(
            @Nonnull final CustomObject<NonResolvedReferencesCustomObject> customObject);

}