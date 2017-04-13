package com.commercetools.sync.inventory.impl;

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

import static java.util.Collections.singleton;

final class InventoryServiceImpl implements InventoryService {

    private final BlockingSphereClient ctpClient;

    public InventoryServiceImpl(BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Override
    public List<InventoryEntry> fetchInventoryEntriesBySkus(Set<String> skus) {
        final InventoryEntryQuery query = InventoryEntryQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.sku().isIn(skus))
                .plusExpansionPaths(expansionModel -> expansionModel.supplyChannel())
                .build();
        return QueryExecutionUtils.queryAll(ctpClient, query)
                .toCompletableFuture()
                .join();
    }

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

    @Override
    public Channel createSupplyChannel(String key) {
        final ChannelDraft draft = ChannelDraftBuilder.of(key)
                .roles(singleton(ChannelRole.INVENTORY_SUPPLY))
                .build();
        return ctpClient.executeBlocking(ChannelCreateCommand.of(draft));
    }

    @Override
    @Nullable
    public InventoryEntry createInventoryEntry(@Nonnull InventoryEntryDraft inventoryEntryDraft) {
        return ctpClient.executeBlocking(InventoryEntryCreateCommand.of(inventoryEntryDraft));
    }

    @Override
    @Nullable
    public InventoryEntry updateInventoryEntry(@Nonnull InventoryEntry inventoryEntry,
                                               @Nonnull List<UpdateAction<InventoryEntry>> updateActions) {
        return ctpClient.executeBlocking(InventoryEntryUpdateCommand.of(inventoryEntry, updateActions));
    }
}
