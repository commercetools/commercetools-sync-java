package com.commercetools.sync.integration.commons.utils;

import com.commercetools.sync.commons.models.WaitingToBeResolved;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.customobjects.CustomObject;
import io.sphere.sdk.customobjects.commands.CustomObjectDeleteCommand;
import io.sphere.sdk.customobjects.queries.CustomObjectQuery;
import io.sphere.sdk.queries.PagedQueryResult;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class CustomObjectITUtils {

    private static final String CUSTOM_OBJECT_CONTAINER_KEY =
        "commercetools-sync-java.UnresolvedReferencesService.productDrafts";

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
                .byContainer(CUSTOM_OBJECT_CONTAINER_KEY);

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

    private CustomObjectITUtils() {
    }
}
