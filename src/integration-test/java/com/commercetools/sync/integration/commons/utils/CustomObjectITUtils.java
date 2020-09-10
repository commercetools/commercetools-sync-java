package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import com.commercetools.sync.commons.models.WaitingToBeResolvedTransitions;
import com.fasterxml.jackson.databind.JsonNode;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.CustomObjectDraft;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.commands.CustomObjectUpsertCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.customobjects.queries.CustomObjectQueryBuilder;
import io.sphere.sdk.queries.PagedQueryResult;


import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public final class CustomObjectITUtils {

    private static final String PRODUCT_CUSTOM_OBJECT_CONTAINER_KEY =
        "commercetools-sync-java.UnresolvedReferencesService.productDrafts";



    /**
     * Deletes customObjects from CTP project which have key/container used in integration test,
     * represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the custom objects will be deleted from.
     */
    public static void deleteCustomObject(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final String key,
        @Nonnull final String container) {

        final CustomObjectQuery<JsonNode> customObjectQuery =
                CustomObjectQueryBuilder.ofJsonNode()
                        .plusPredicates(queryModel -> queryModel.container().is(container))
                        .plusPredicates(queryModel -> queryModel.key().is(key))
                        .build();

        ctpClient
                .execute(customObjectQuery)
                .thenApply(PagedQueryResult::getResults)
                .thenCompose(customObjects -> deleteCustomObject(ctpClient, customObjects))
                .toCompletableFuture()
                .join();
    }

    @Nonnull
    private static CompletableFuture<Void> deleteCustomObject(
            @Nonnull final SphereClient ctpClient,
            @Nonnull final List<CustomObject<JsonNode>> customObjects) {

        return CompletableFuture.allOf(
                customObjects
                        .stream()
                        .map(customObject -> ctpClient
                                .execute(CustomObjectDeleteCommand.of(customObject, JsonNode.class)))
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new));
    }

    /**
     * Create a custom object in target CTP project, represented by provided {@code ctpClient}.
     *
     * @param ctpClient represents the CTP project the custom object will be created.
     * @param key represents the key field required in custom object.
     * @param container represents the container field required in custom object.
     * @param value represents the value field required in custom object.
     */
    public static CustomObject<JsonNode> createCustomObject(
            @Nonnull final SphereClient ctpClient,
            @Nonnull final String key,
            @Nonnull final String container,
            @Nonnull final JsonNode value) {

        CustomObjectDraft<JsonNode> customObjectDraft = CustomObjectDraft.ofUnversionedUpsert(container,key, value);
        return ctpClient.execute(CustomObjectUpsertCommand.of(customObjectDraft)).toCompletableFuture().join();
    }

    /**
     * This method is expected to be used only by tests, it only works on projects with less than or equal to 20 custom
     * objects. Otherwise, it won't delete all the custom objects in the project of the client.
     *
     * @param ctpClient the client to delete the custom objects from.
     */
    public static void deleteWaitingToBeResolvedCustomObjects(@Nonnull final SphereClient ctpClient) {

        final CustomObjectQuery<WaitingToBeResolved> customObjectQuery =
            CustomObjectQuery
                .of(WaitingToBeResolved.class)
                .byContainer(PRODUCT_CUSTOM_OBJECT_CONTAINER_KEY);

        ctpClient
            .execute(customObjectQuery)
            .thenApply(PagedQueryResult::getResults)
            .thenCompose(customObjects -> deleteWaitingToBeResolvedCustomObjects(ctpClient, customObjects))
            .toCompletableFuture()
            .join();
    }

    @Nonnull
    private static CompletableFuture<Void> deleteWaitingToBeResolvedCustomObjects(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final List<CustomObject<WaitingToBeResolved>> customObjects) {

        return CompletableFuture.allOf(
            customObjects
                .stream()
                .map(customObject -> ctpClient
                    .execute(CustomObjectDeleteCommand.of(customObject, WaitingToBeResolved.class)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new));
    }

    public static void deleteWaitingToBeResolvedTransitionsCustomObjects(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final String customObjectKey) {

        final CustomObjectQuery<WaitingToBeResolvedTransitions> customObjectQuery =
            CustomObjectQuery
                .of(WaitingToBeResolvedTransitions.class)
                .byContainer(customObjectKey);

        ctpClient
            .execute(customObjectQuery)
            .thenApply(PagedQueryResult::getResults)
            .thenCompose(customObjects -> deleteWaitingToBeResolvedTransitionsCustomObjects(ctpClient, customObjects))
            .toCompletableFuture()
            .join();
    }

    @Nonnull
    private static CompletableFuture<Void> deleteWaitingToBeResolvedTransitionsCustomObjects(
        @Nonnull final SphereClient ctpClient,
        @Nonnull final List<CustomObject<WaitingToBeResolvedTransitions>> customObjects) {

        return CompletableFuture.allOf(
            customObjects
                .stream()
                .map(customObject -> ctpClient
                    .execute(CustomObjectDeleteCommand.of(customObject, WaitingToBeResolvedTransitions.class)))
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new));
    }

    private CustomObjectITUtils() {
    }
}
