package com.commercetools.sync.inventory;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelDraftBuilder;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryUpdateCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryBuilder;
import io.sphere.sdk.queries.QueryExecutionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.singleton;

final class InventoryServiceImpl implements InventoryService {

    private final BlockingSphereClient ctpClient;

    public InventoryServiceImpl(BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Nonnull
    @Override
    public List<InventoryEntry> fetchInventoryEntriesBySkus(@Nonnull final Set<String> skus) {
        final InventoryEntryQuery query = InventoryEntryQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.sku().isIn(skus))
                .plusExpansionPaths(expansionModel -> expansionModel.supplyChannel())
                .build();
        return QueryExecutionUtils.queryAll(ctpClient, query)
                .toCompletableFuture()
                .join();
    }

    @Nonnull
    @Override
    public List<Channel> fetchAllSupplyChannels() {
        final ChannelQuery query = ChannelQueryBuilder.of()
                .plusPredicates(channelQueryModel -> channelQueryModel.roles()
                        .containsAny(Collections.singletonList(ChannelRole.INVENTORY_SUPPLY)))
                .build();
        return QueryExecutionUtils.queryAll(ctpClient, query)
                .toCompletableFuture()
                .join();
    }

    @Nullable
    @Override
    public Channel createSupplyChannel(@Nonnull final String key) {
        final ChannelDraft draft = ChannelDraftBuilder.of(key)
                .roles(singleton(ChannelRole.INVENTORY_SUPPLY))
                .build();
        return ctpClient.executeBlocking(ChannelCreateCommand.of(draft));
    }

    @Override
    public CompletionStage<InventoryEntry> createInventoryEntry(@Nonnull final InventoryEntryDraft inventoryEntryDraft) {
        return ctpClient.execute(InventoryEntryCreateCommand.of(inventoryEntryDraft));
    }

    @Override
    public CompletionStage<InventoryEntry> updateInventoryEntry(@Nonnull final InventoryEntry inventoryEntry,
                                                                @Nonnull final List<UpdateAction<InventoryEntry>> updateActions) {
        return ctpClient.execute(InventoryEntryUpdateCommand.of(inventoryEntry, updateActions));
    }
}
