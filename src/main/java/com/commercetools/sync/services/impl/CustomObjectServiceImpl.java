package com.commercetools.sync.services.impl;

import com.commercetools.sync.customobjects.CustomObjectSyncOptions;
import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.commercetools.sync.services.CustomObjectService;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.expansion.CustomObjectExpansionModel;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.customobjects.queries.CustomObjectQueryBuilder;
import io.sphere.sdk.customobjects.queries.CustomObjectQueryModel;
import io.sphere.sdk.queries.QueryPredicate;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Implementation of CustomObjectService interface.
 */

public class CustomObjectServiceImpl
    extends BaseService<CustomObjectDraft<JsonNode>,
    CustomObject<JsonNode>,
    CustomObjectSyncOptions,
    CustomObjectQuery<JsonNode>,
    CustomObjectQueryModel<CustomObject<JsonNode>>, CustomObjectExpansionModel<CustomObject<JsonNode>>>
        implements CustomObjectService {

    public CustomObjectServiceImpl(@Nonnull final CustomObjectSyncOptions syncOptions) {
        super(syncOptions);
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<String>> fetchCachedCustomObjectId(
            @Nonnull final CustomObjectCompositeIdentifier identifier) {

        String container = identifier.getContainer();
        String key = identifier.getKey();

        if (StringUtils.isEmpty(container) || StringUtils.isEmpty(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return fetchCachedResourceId(identifier.toString(),
            draft -> CustomObjectCompositeIdentifier.of(draft).toString(),
            () -> CustomObjectQuery.ofJsonNode()
                        .withPredicates(q -> q.container().is(container).and(q.key().is(key)))
        );
    }

    @Nonnull
    @Override
    public CompletionStage<Set<CustomObject<JsonNode>>> fetchMatchingCustomObjects(
            @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {

        Set<CustomObjectCompositeIdentifier> filteredIdentifiers = identifiers.stream()
                .filter(identifier ->
                            StringUtils.isNotEmpty(identifier.getContainer())
                        &&  StringUtils.isNotEmpty(identifier.getKey()))
                .collect(Collectors.toSet());

        if (filteredIdentifiers.size() == 0) {
            return CompletableFuture.completedFuture(Collections.emptySet());
        }

        Set<String> identifierStrings =
                filteredIdentifiers.stream()
                        .map(CustomObjectCompositeIdentifier::toString)
                        .collect(Collectors.toSet());

        return fetchMatchingResources(identifierStrings,
            draft -> CustomObjectCompositeIdentifier.of(draft).toString(),
            () -> {
                CustomObjectQueryBuilder<JsonNode> query = CustomObjectQueryBuilder.ofJsonNode();
                query = query.plusPredicates(queryModel -> createQuery(queryModel, filteredIdentifiers));
                return query.build();
            });
    }

    @Nonnull
    private QueryPredicate<CustomObject<JsonNode>> createQuery(
            @Nonnull final CustomObjectQueryModel<CustomObject<JsonNode>> queryModel,
            @Nonnull final Set<CustomObjectCompositeIdentifier> identifiers) {

        QueryPredicate<CustomObject<JsonNode>> queryPredicate = QueryPredicate.of(null);
        boolean firstAttempt = true;
        for (CustomObjectCompositeIdentifier identifier : identifiers) {
            String key = identifier.getKey();
            String container = identifier.getContainer();
            if (firstAttempt) {
                queryPredicate = queryModel.container().is(container).and(queryModel.key().is(key));
                firstAttempt = false;
            } else {
                queryPredicate = queryPredicate.or(queryModel.container().is(container).and(queryModel.key().is(key)));
            }
        }
        return queryPredicate;
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<JsonNode>>> fetchCustomObject(
            @Nonnull final CustomObjectCompositeIdentifier identifier) {

        String container = identifier.getContainer();
        String key = identifier.getKey();

        if (StringUtils.isEmpty(container) || StringUtils.isEmpty(key)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return fetchResource(identifier.toString(),
            () -> CustomObjectQuery.ofJsonNode()
                                   .withPredicates(q -> q.container().is(container).and(q.key().is(key)))
        );
    }

    @Nonnull
    @Override
    public CompletionStage<Optional<CustomObject<JsonNode>>> upsertCustomObject(
        @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {
        if (StringUtils.isEmpty(customObjectDraft.getKey()) || StringUtils.isEmpty(customObjectDraft.getContainer())) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return createResource(customObjectDraft,
            draft -> CustomObjectCompositeIdentifier.of(draft).toString(),
                CustomObjectUpsertCommand::of);
    }
}
