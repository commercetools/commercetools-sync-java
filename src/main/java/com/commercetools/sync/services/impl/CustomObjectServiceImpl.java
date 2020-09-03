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
import io.sphere.sdk.customobjects.queries.CustomObjectQueryModel;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

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
    public CompletionStage<Optional<String>> fetchCachedCustomObjectId(
            @Nonnull final CustomObjectCompositeIdentifier identifier) {

        String container = identifier.getContainer();
        String key = identifier.getKey();

        return fetchCachedResourceId(identifier.toString(),
                draft -> CustomObjectCompositeIdentifier.of(draft).toString(),
                () -> CustomObjectQuery.ofJsonNode()
                        .withPredicates(q -> q.container().is(container).and(q.key().is(key)))
        );
    }

    @Nonnull
    public CompletionStage<Set<CustomObject<JsonNode>>> fetchMatchingCustomObjectByKeys(
            @Nonnull final  Set<CustomObjectCompositeIdentifier> identifiers) {
        // TODO
        return null;
    }
    
    @Nonnull
    public CompletionStage<Optional<CustomObject<JsonNode>>> fetchCustomObject(
            @Nonnull final CustomObjectCompositeIdentifier identifier) {

        String container = identifier.getContainer();
        String key = identifier.getKey();

        return fetchResource(identifier.toString(),
            () -> CustomObjectQuery.ofJsonNode()
                                   .withPredicates(q -> q.container().is(container).and(q.key().is(key)))
        );
    }

    @Nonnull
    public CompletionStage<Optional<CustomObject<JsonNode>>> upsertCustomObject(
        @Nonnull final CustomObjectDraft<JsonNode> customObjectDraft) {

        return createResource(customObjectDraft,
                draft -> CustomObjectCompositeIdentifier.of(draft).toString(),
                CustomObjectUpsertCommand::of);
    }
}
