package com.commercetools.sync.integration.commons.utils;

import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.stores.Store;
import io.sphere.sdk.stores.StoreDraft;
import io.sphere.sdk.stores.StoreDraftBuilder;
import io.sphere.sdk.stores.commands.StoreCreateCommand;
import io.sphere.sdk.stores.commands.StoreDeleteCommand;
import io.sphere.sdk.stores.queries.StoreQuery;

import javax.annotation.Nonnull;

import static com.commercetools.sync.integration.commons.utils.ITUtils.queryAndExecute;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.tests.utils.CompletionStageUtil.executeBlocking;

public final class StoreITUtils {
    /**
     * Deletes all stores from CTP projects defined by the {@code CTP_SOURCE_CLIENT} and
     * {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteStoresFromTargetAndSource() {
        deleteStores(CTP_TARGET_CLIENT);
        deleteStores(CTP_SOURCE_CLIENT);
    }

    /**
     * Deletes all stores from the CTP project defined by the {@code ctpClient}.
     *
     * @param ctpClient defines the CTP project to delete the stores from.
     */
    public static void deleteStores(@Nonnull final SphereClient ctpClient) {
        queryAndExecute(ctpClient, StoreQuery.of(), StoreDeleteCommand::of);
    }

    /**
     * Creates a {@link Store} in the CTP project defined by the {@code ctpClient} in a blocking fashion.
     *
     * @param ctpClient defines the CTP project to create the Store in.
     * @param key       the key of the Store to create.
     * @return the created store.
     */
    public static Store createStore(@Nonnull final SphereClient ctpClient, @Nonnull final String key) {
        final StoreDraft storeDraft = StoreDraftBuilder.of(key).build();
        return executeBlocking(ctpClient.execute(StoreCreateCommand.of(storeDraft)));
    }

    private StoreITUtils() {
    }
}
