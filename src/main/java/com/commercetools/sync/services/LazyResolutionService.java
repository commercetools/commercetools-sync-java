package com.commercetools.sync.services;


import com.commercetools.sync.commons.models.NonResolvedReferencesCustomObject;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface LazyResolutionService {

    /**
     * Given a resource key, this method fetches the matching resource. If there is no matching resource
     * object an empty {@link Optional} will be returned in the returned future.
     *
     * @param key the key of the custom object to fetch.
     * @return {@link CompletionStage}&lt;{@link Optional}&gt; in which the result of it's completion contains an
     * {@link Optional} that contains the matching {@link CustomObject} if exists, otherwise empty.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>> fetch(
            @Nullable final String key);

    /**
     * Given a resource gets created/updated on CTP, then returns the created resource
     * of {@link CompletionStage} in which contains an instance {@link Optional}
     * of the resource which was created/updated.
     *
     * @param customObjectDraft the resource to create/update a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the created resource if successful otherwise an
     *     empty optional.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>> save(
            @Nonnull final CustomObjectDraft<NonResolvedReferencesCustomObject> customObjectDraft);

    /**
     * Given a resource gets deleted on CTP, then returns deleted resource
     * of {@link CompletionStage} in which contains an instance {@link Optional}
     * of the resource which was deleted.
     *
     * @param customObject the resource to delete a resource based off of.
     * @return a {@link CompletionStage} containing an optional with the deleted resource if successful otherwise an
     *     empty optional.
     */
    @Nonnull
    CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>> delete(
            @Nonnull final CustomObject<NonResolvedReferencesCustomObject> customObject);

}