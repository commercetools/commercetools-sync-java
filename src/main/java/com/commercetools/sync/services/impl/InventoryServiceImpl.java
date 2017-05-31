package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.InventoryService;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryUpdateCommand;
import io.sphere.sdk.inventory.expansion.InventoryEntryExpansionModel;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryBuilder;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class InventoryServiceImpl implements InventoryService {

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
                                                                    .plusExpansionPaths(
                                                                        InventoryEntryExpansionModel::supplyChannel)
                                                                    .build();
        return QueryExecutionUtils.queryAll(ctpClient, query);
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
