package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.InventoryService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryUpdateCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryBuilder;
import io.sphere.sdk.models.Referenceable;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public final class InventoryServiceImpl implements InventoryService {

    private final SphereClient ctpClient;

    public InventoryServiceImpl(@Nonnull final SphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public CompletionStage<List<InventoryEntry>> fetchInventoryEntriesBySkus(@Nonnull final Set<String> skus) {
        final InventoryEntryQuery query = InventoryEntryQueryBuilder.of()
                                                                    .plusPredicates(
                                                                        queryModel -> queryModel.sku().isIn(skus))
                                                                    .build();
        return QueryExecutionUtils.queryAll(ctpClient, query);
    }

    @Override
    public CompletionStage<Optional<InventoryEntry>> fetchInventoryEntry(@Nonnull final String sku,
                                                                         @Nullable final Referenceable<Channel>
                                                                             supplyChannel) {
        InventoryEntryQuery query = InventoryEntryQuery.of().plusPredicates(inventoryEntryQueryModel ->
            inventoryEntryQueryModel.sku().is(sku));
        query = supplyChannel == null
            ? query.plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel().isNotPresent())
            : query.plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel()
            .is(supplyChannel));
        return ctpClient.execute(query).thenApply(pagedResult -> pagedResult.head());
    }

    @Nonnull
    @Override
    public CompletionStage<InventoryEntry> createInventoryEntry(@Nonnull final InventoryEntryDraft
                                                                    inventoryEntryDraft) {
        return ctpClient.execute(InventoryEntryCreateCommand.of(inventoryEntryDraft));
    }

    @Nonnull
    @Override
    public CompletionStage<InventoryEntry> updateInventoryEntry(@Nonnull final InventoryEntry inventoryEntry,
                                                                @Nonnull final List<UpdateAction<InventoryEntry>>
                                                                    updateActions) {
        return ctpClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateActions));
    }
}
