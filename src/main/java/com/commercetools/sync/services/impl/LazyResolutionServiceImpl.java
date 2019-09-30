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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class LazyResolutionServiceImpl
        implements LazyResolutionService {

    private final ProductSyncOptions productSyncOptions;

    private static final String CUSTOM_OBJECT_CONTAINER_KEY = "commercetools-sync-java.LazyResolutionService";

    private static final String SAVE_FAILED = "Failed to save draft with key: '%s'. Reason: %s";

    private static final String DELETE_FAILED = "Failed to delete resource with key: '%s'. Reason: %s";

    public LazyResolutionServiceImpl(final ProductSyncOptions productSyncOptions) {
        this.productSyncOptions = productSyncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<ProductWithUnResolvedProductReferences>>>
        fetch(@Nullable final String key) {

        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return productSyncOptions
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
                .ofUnversionedUpsert(CUSTOM_OBJECT_CONTAINER_KEY,
                        Objects.requireNonNull(draftWithUnresolvedReferences.getProductDraft().getKey()),
                        draftWithUnresolvedReferences, ProductWithUnResolvedProductReferences.class);

        return productSyncOptions
                .getCtpClient()
                .execute(CustomObjectUpsertCommand.of(customObjectDraft))
                .handle((resource, exception) -> {
                    if (exception == null) {
                        return Optional.of(resource);
                    } else {
                        productSyncOptions.applyErrorCallback(
                                format(SAVE_FAILED, customObjectDraft.getKey(), exception.getMessage()), exception);
                        return Optional.empty();
                    }
                });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<ProductWithUnResolvedProductReferences>>>
        delete(@Nonnull final String nonResolvedReferencesObjectKey) {

        return productSyncOptions
                .getCtpClient()
                .execute(CustomObjectDeleteCommand
                        .of(CUSTOM_OBJECT_CONTAINER_KEY, nonResolvedReferencesObjectKey,
                                ProductWithUnResolvedProductReferences.class))
                .handle((resource, exception) -> {
                    if (exception == null) {
                        return Optional.of(resource);
                    } else {
                        productSyncOptions.applyErrorCallback(
                                format(DELETE_FAILED, nonResolvedReferencesObjectKey,
                                        exception.getMessage()), exception);
                        return Optional.empty();
                    }
                });
    }
}
