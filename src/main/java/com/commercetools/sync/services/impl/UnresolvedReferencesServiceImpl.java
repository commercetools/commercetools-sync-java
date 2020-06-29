package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.products.ProductSyncOptions;
import com.commercetools.sync.services.UnresolvedReferencesService;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

public class UnresolvedReferencesServiceImpl implements UnresolvedReferencesService {

    private final ProductSyncOptions syncOptions;

    private static final String SAVE_FAILED =
        "Failed to save CustomObject with key: '%s' (hash of product key: '%s').";
    private static final String DELETE_FAILED =
        "Failed to delete CustomObject with key: '%s' (hash of product key: '%s').";
    private static final String CUSTOM_OBJECT_CONTAINER_KEY =
        "commercetools-sync-java.UnresolvedReferencesService.productDrafts";


    public UnresolvedReferencesServiceImpl(@Nonnull final ProductSyncOptions baseSyncOptions) {
        this.syncOptions = baseSyncOptions;
    }

    @Nonnull
    private String hash(@Nullable final String customObjectKey) {
        return sha1Hex(customObjectKey);
    }

    @Nonnull
    @Override
    public CompletionStage<Set<WaitingToBeResolved>> fetch(@Nonnull final Set<String> keys) {

        if (keys.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        Set<String> hashedKeys = keys.stream()
            .map(this::hash).collect(Collectors.toSet());

        final CustomObjectQuery<WaitingToBeResolved> customObjectQuery =
            CustomObjectQuery
                .of(WaitingToBeResolved.class)
                .byContainer(CUSTOM_OBJECT_CONTAINER_KEY)
                .plusPredicates(p -> p.key().isIn(hashedKeys));

        return QueryExecutionUtils
            .queryAll(syncOptions.getCtpClient(), customObjectQuery)
            .thenApply(customObjects -> customObjects
                .stream()
                .map(CustomObject::getValue)
                .collect(toList()))
            .thenApply(HashSet::new);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<WaitingToBeResolved>> save(@Nonnull final WaitingToBeResolved draft) {

        final CustomObjectDraft<WaitingToBeResolved> customObjectDraft = CustomObjectDraft
            .ofUnversionedUpsert(
                CUSTOM_OBJECT_CONTAINER_KEY,
                hash(draft.getProductDraft().getKey()),
                draft,
                WaitingToBeResolved.class);

        return syncOptions
            .getCtpClient()
            .execute(CustomObjectUpsertCommand.of(customObjectDraft))
            .handle((resource, exception) -> {
                if (exception == null) {
                    return Optional.of(resource.getValue());
                } else {
                    syncOptions.applyErrorCallback(
                        new SyncException(format(SAVE_FAILED, customObjectDraft.getKey(),
                                draft.getProductDraft().getKey()), exception));
                    return Optional.empty();
                }
            });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<WaitingToBeResolved>> delete(@Nonnull final String key) {

        return syncOptions
            .getCtpClient()
            .execute(CustomObjectDeleteCommand
                .of(CUSTOM_OBJECT_CONTAINER_KEY, hash(key), WaitingToBeResolved.class))
            .handle((resource, exception) -> {
                if (exception == null) {
                    return Optional.of(resource.getValue());
                } else {
                    syncOptions.applyErrorCallback(
                        new SyncException(format(DELETE_FAILED, hash(key), key), exception));
                    return Optional.empty();
                }
            });
    }
}
