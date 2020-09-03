package com.commercetools.sync.services;

import com.commercetools.sync.customobjects.helpers.CustomObjectCompositeIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface CustomObjectService {


    @Nonnull
    CompletionStage<Optional<String>> fetchCachedCustomObjectId(@Nonnull CustomObjectCompositeIdentifier identifier);

    @Nonnull
    CompletionStage<Set<CustomObject<JsonNode>>> fetchMatchingCustomObjectByKeys(@Nonnull Set<CustomObjectCompositeIdentifier> identifiers);

    @Nonnull
    CompletionStage<Optional<CustomObject<JsonNode>>> fetchCustomObject(@Nonnull CustomObjectCompositeIdentifier identifier);

    @Nonnull
    CompletionStage<Optional<CustomObject<JsonNode>>> upsertCustomObject(@Nonnull CustomObjectDraft<JsonNode> customObjectDraft);
}
