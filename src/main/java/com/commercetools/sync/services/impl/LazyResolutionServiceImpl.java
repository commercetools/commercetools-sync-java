package com.commercetools.sync.services.impl;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.models.NonResolvedReferencesCustomObject;
import com.commercetools.sync.services.LazyResolutionService;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedResult;
import io.sphere.sdk.queries.QueryPredicate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class LazyResolutionServiceImpl
        implements LazyResolutionService {

    private final BaseSyncOptions syncOptions;

    private static final String CREATE_FAILED = "Failed to create draft with key: '%s'. Reason: %s";

    public LazyResolutionServiceImpl(final BaseSyncOptions baseSyncOptions) {
        this.syncOptions = baseSyncOptions;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>>
    fetch(@Nullable final String key) {

        if (isBlank(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        final CustomObjectQuery<NonResolvedReferencesCustomObject> customObjectQuery = CustomObjectQuery
                .of(NonResolvedReferencesCustomObject.class)
                .withPredicates(buildCustomObjectKeysQueryPredicate(singleton(key)));

        return syncOptions
                .getCtpClient()
                .execute(customObjectQuery)
                .thenApply(PagedResult::head);
    }

    private QueryPredicate<CustomObject<NonResolvedReferencesCustomObject>> buildCustomObjectKeysQueryPredicate(
        @Nonnull final Set<String> customObjectKeys) {
        final List<String> keysSurroundedWithDoubleQuotes = customObjectKeys.stream()
                .filter(StringUtils::isNotBlank)
                .map(customObjectKey -> format("\"%s\"", customObjectKey))
                .collect(Collectors.toList());
        String keysQueryString = keysSurroundedWithDoubleQuotes.toString();
        // Strip square brackets from list string. For example: ["key1", "key2"] -> "key1", "key2"
        keysQueryString = keysQueryString.substring(1, keysQueryString.length() - 1);
        return QueryPredicate.of(format("key in (%s)", keysQueryString));
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>>
        createOrUpdateCustomObject(@Nonnull final CustomObjectDraft<NonResolvedReferencesCustomObject>
                                           customObjectDraft) {

        return syncOptions
                .getCtpClient()
                .execute(CustomObjectUpsertCommand.of(customObjectDraft))
                .handle((resource, exception) -> {
                    if (exception == null) {
                        return Optional.of(resource);
                    } else {
                        syncOptions.applyErrorCallback(
                                format(CREATE_FAILED, customObjectDraft.getKey(), exception.getMessage()), exception);
                        return Optional.empty();
                    }
                });
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<NonResolvedReferencesCustomObject>>>
        deleteCustomObject(@Nonnull final CustomObject<NonResolvedReferencesCustomObject> customObject) {

        return syncOptions
                .getCtpClient()
                .execute(CustomObjectDeleteCommand.of(customObject, NonResolvedReferencesCustomObject.class))
                .handle((resource, exception) -> {
                    if (exception == null) {
                        return Optional.of(resource);
                    } else {
                        syncOptions.applyErrorCallback(
                                format(CREATE_FAILED, customObject.getKey(), exception.getMessage()), exception);
                        return Optional.empty();
                    }
                });
    }
}
