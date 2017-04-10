package com.commercetools.sync.services.impl;

import com.commercetools.sync.services.InventoryService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.inventory.queries.InventoryEntryQueryBuilder;
import io.sphere.sdk.models.Identifiable;
import io.sphere.sdk.queries.*;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//TODO implement one general method instead of findNextInventories and findNextChannels
public final class InventoryServiceImpl implements InventoryService {

    //Override default page limit to have greater chance of fetching all in one query
    private final static int PAGE_SIZE = 200;

    private final BlockingSphereClient ctpClient;

    public InventoryServiceImpl(BlockingSphereClient ctpClient) {
        this.ctpClient = ctpClient;
    }

    @Override
    public List<InventoryEntry> fetchInventoryEntriesBySkus(Set<String> skus) {
        final List<InventoryEntry> inventories = new LinkedList<>();
        final InventoryEntryQuery seedQuery =
                getQueryForFetchingInventoryEntiresBySkus(skus);
        return findNextInventories(seedQuery, seedQuery, inventories);
    }

    @Override
    public List<Channel> fetchAllSupplyChannels() {
        final List<Channel> channels = new LinkedList<>();
        final ChannelQuery seedQuery =
                getQueryForFetchingAllSupplyChannels();
        return findNextChannels(seedQuery, seedQuery, channels);
    }

    @Override
    public InventoryEntry createInventoryEntry(@Nonnull InventoryEntryDraft inventoryEntryDraft) {
        //TODO implement
        return null;
    }

    @Override
    public InventoryEntry updateInventoryEntry(@Nonnull InventoryEntry inventoryEntry, @Nonnull List<UpdateAction<InventoryEntry>> updateActions) {
        //TODO implement
        return null;
    }

    /**
     * Get query for fetching inventory entries by sku in {@code skus}. The query additionally:
     * <ul>
     *     <li>demands expansion of supply {@link Channel}</li>
     *     <li>is sorted by {@code id} ASC</li>
     *     <li>is limited to {@link InventoryServiceImpl#PAGE_SIZE}</li>
     *     <li>have {@code fetchTotal} flag disabled</li>
     * </ul>
     * @param skus
     * @return builded query
     */
    private InventoryEntryQuery getQueryForFetchingInventoryEntiresBySkus(@Nonnull final Set<String> skus) {
        return InventoryEntryQueryBuilder.of()
                .plusPredicates(queryModel -> queryModel.sku().isIn(skus))
                .plusExpansionPaths(expansionModel -> expansionModel.supplyChannel())
                .plusSort(queryModel -> queryModel.id().sort().asc())
                .limit(PAGE_SIZE)
                .fetchTotal(false)
                .build();
    }

    /**
     * Get query for fetching all {@link Channel} of role {@link ChannelRole#INVENTORY_SUPPLY}. The query additionally:
     * <ul>
     *     <li>is sorted by {@code id} ASC</li>
     *     <li>is limited to {@link InventoryServiceImpl#PAGE_SIZE}</li>
     *     <li>have {@code fetchTotal} flag disabled</li>
     * </ul>
     * @return builded query
     */
    private ChannelQuery getQueryForFetchingAllSupplyChannels() {
        return ChannelQueryBuilder.of()
                .plusPredicates(channelQueryModel -> channelQueryModel.roles()
                        .containsAny(Collections.singletonList(ChannelRole.INVENTORY_SUPPLY)))
                .plusSort(queryModel -> queryModel.id().sort().asc())
                .limit(PAGE_SIZE)
                .fetchTotal(false)
                .build();
    }

    /**
     * Returns all results for given {@code seedQuery}. As results from sphere are paginated, this method
     * calls itself recursively to fetch all result pages for given {@code seedQuery} and returns {@link List}
     * containing merged results from all pages.
     * @param seedQuery query to fetch entries. Should be sorted by ID ascending, and limited
     *                  to {@link InventoryServiceImpl#PAGE_SIZE}
     * @param query actually query that is performed, for recursion reasons. Should be same as {@code seedQuery}
     *              in initial call
     * @param inventories {@link List} to which page results should be collected.
     * @return all entries matching to query
     */
    private List<InventoryEntry> findNextInventories(final InventoryEntryQuery seedQuery,
                                                     final InventoryEntryQuery query,
                                                     final List<InventoryEntry> inventories) {
        final List<InventoryEntry> result = ctpClient.executeBlocking(query).getResults();
        final boolean isLastPage = result.size() < PAGE_SIZE;
        inventories.addAll(result);
        if (isLastPage) {
            return inventories;
        } else {
            final String lastId = getIdFromLastElement(result);
            return findNextInventories(seedQuery, seedQuery.plusPredicates(m -> m.id().isGreaterThan(lastId)),
                    inventories);
        }
    }

    /**
     * Analogue as {@link InventoryServiceImpl#findNextInventories(InventoryEntryQuery, InventoryEntryQuery, List)}
     */
    private List<Channel> findNextChannels(final ChannelQuery seedQuery,
                                           final ChannelQuery query,
                                           final List<Channel> channels) {
        final List<Channel> result = ctpClient.executeBlocking(query).getResults();
        final boolean isLastPage = result.size() < PAGE_SIZE;
        channels.addAll(result);
        if (isLastPage) {
            return channels;
        } else {
            final String lastId = getIdFromLastElement(result);
            return findNextChannels(seedQuery, seedQuery.plusPredicates(m -> m.id().isGreaterThan(lastId)), channels);
        }
    }

    /**
     * Gets the ID of last element in a list.
     * As parameter should be passed not empty {@link List} that is extracted from {@link PagedResult}, that was
     * a result of Query sorted by ID in ascending order.
     * @param pageResult not empty list extracted from {@link PagedResult}
     * @return the last ID
     */
    private <T extends Identifiable<T>> String getIdFromLastElement(final List<T> pageResult) {
        final int indexLastElement = pageResult.size() - 1;
        return pageResult.get(indexLastElement).getId();
    }
}
