package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.models.NonResolvedReferencesCustomObject;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.LazyResolutionService;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectByKeyGet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class LazyResolutionServiceImpl
        implements LazyResolutionService {

    private final ProductSyncOptions productSyncOptions;

    private static final String CUSTOM_OBJECT_CONTAINER_KEY = "commercetools-sync-java.LazyResolutionService";

    private static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";

    public LazyResolutionServiceImpl(final ProductSyncOptions productSyncOptions) {
        this.productSyncOptions = productSyncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>>
    fetch(@Nullable final String key) {

        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return productSyncOptions
                .getCtpClient()
                .execute(CustomObjectByKeyGet
                        .of(CUSTOM_OBJECT_CONTAINER_KEY, key, NonResolvedReferencesCustomObject.class))
                .thenApply(Optional::ofNullable);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>>
    save(@Nonnull final NonResolvedReferencesCustomObject nonResolvedReferencesCustomObject) {

        CustomObjectDraft<NonResolvedReferencesCustomObject> customObjectDraft = CustomObjectDraft
                .ofVersionedUpsert(CUSTOM_OBJECT_CONTAINER_KEY,
                nonResolvedReferencesCustomObject.getProductKey(),
                nonResolvedReferencesCustomObject, 1L, NonResolvedReferencesCustomObject.class);

        return productSyncOptions
                .getCtpClient()
                .execute(CustomObjectUpsertCommand.of(customObjectDraft))
                .handle((resource, exception) -> {
                    if (exception == null) {
                        return Optional.of(resource);
                    } else {
                        productSyncOptions.applyErrorCallback(
                                format(CREATE_FAILED, customObjectDraft.getKey(), exception.getMessage()), exception);
                        return Optional.empty();
                    }
                });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>>
    delete(@Nonnull final CustomObject<NonResolvedReferencesCustomObject> customObject) {

        return productSyncOptions
                .getCtpClient()
                .execute(CustomObjectDeleteCommand.of(customObject, NonResolvedReferencesCustomObject.class))
                .handle((resource, exception) -> {
                    if (exception == null) {
                        return Optional.of(resource);
                    } else {
                        productSyncOptions.applyErrorCallback(
                                format(CREATE_FAILED, customObject.getKey(), exception.getMessage()), exception);
                        return Optional.empty();
                    }
                });
    }
}
