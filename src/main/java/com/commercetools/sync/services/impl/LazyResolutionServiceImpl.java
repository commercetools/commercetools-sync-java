package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.models.ProductWithUnResolvedProductReferences;
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

public class LazyResolutionServiceImpl implements LazyResolutionService {

    private final ProductSyncOptions syncOptions;

    private static final String SAVE_FAILED = "Failed to save CustomObject with key: '%s'. Reason: %s";
    private static final String DELETE_FAILED = "Failed to delete CustomObject with key: '%s'. Reason: %s";
    private static final String CUSTOM_OBJECT_CONTAINER_KEY = "commercetools-sync-java.LazyResolutionService";

    public LazyResolutionServiceImpl(final ProductSyncOptions baseSyncOptions) {
        this.syncOptions = baseSyncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<ProductWithUnResolvedProductReferences>>>
    fetch(@Nullable final String key) {

        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return syncOptions
            .getCtpClient()
            .execute(CustomObjectByKeyGet
                .of(CUSTOM_OBJECT_CONTAINER_KEY, key, ProductWithUnResolvedProductReferences.class))
            .thenApply(Optional::ofNullable);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<ProductWithUnResolvedProductReferences>>>
    save(@Nonnull final ProductWithUnResolvedProductReferences draftWithUnresolvedReferences) {


        final CustomObjectDraft<ProductWithUnResolvedProductReferences> customObjectDraft = CustomObjectDraft
            .ofUnversionedUpsert(
                CUSTOM_OBJECT_CONTAINER_KEY,
                draftWithUnresolvedReferences.getProductDraft().getKey(),
                draftWithUnresolvedReferences,
                ProductWithUnResolvedProductReferences.class);

        return syncOptions
            .getCtpClient()
            .execute(CustomObjectUpsertCommand.of(customObjectDraft))
            .handle((resource, exception) -> {
                if (exception == null) {
                    return Optional.of(resource);
                } else {
                    syncOptions.applyErrorCallback(
                        format(SAVE_FAILED, customObjectDraft.getKey(), exception.getMessage()), exception);
                    return Optional.empty();
                }
            });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<ProductWithUnResolvedProductReferences>>>
    delete(@Nonnull final String key) {
        return syncOptions
            .getCtpClient()
            .execute(CustomObjectDeleteCommand
                .of(CUSTOM_OBJECT_CONTAINER_KEY, key, ProductWithUnResolvedProductReferences.class))
            .handle((resource, exception) -> {
                if (exception == null) {
                    return Optional.of(resource);
                } else {
                    syncOptions.applyErrorCallback(
                        format(DELETE_FAILED, key, exception.getMessage()), exception);
                    return Optional.empty();
                }
            });
    }
}
