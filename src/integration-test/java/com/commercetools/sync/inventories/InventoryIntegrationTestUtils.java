package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.cleanupTable;
import static java.util.Collections.singleton;

class InventoryIntegrationTestUtils {

    /**
     * Deletes up to {@link com.commercetools.sync.commons.utils.SphereClientUtils#QUERY_MAX_LIMIT} inventory entries
     * from CTP project, represented by provided {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     */
    static void cleanupInventoryEntries(@Nonnull final BlockingSphereClient sphereClient) {
        cleanupTable(sphereClient, InventoryIntegrationTestUtils::inventoryEntryQuerySupplier,
            InventoryEntryDeleteCommand::of);
    }

    /**
     * Deletes up to {@link com.commercetools.sync.commons.utils.SphereClientUtils#QUERY_MAX_LIMIT} channels containing
     * {@link ChannelRole#INVENTORY_SUPPLY} role from CTP project, represented by provided {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     */
    static void cleanupSupplyChannels(@Nonnull final BlockingSphereClient sphereClient) {
        cleanupTable(sphereClient, InventoryIntegrationTestUtils::supplyChannelQuerySupplier,
            ChannelDeleteCommand::of);
    }

    /**
     * Tries to fetch inventory entry of {@code sku} and {@code supplyChannel} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     * @param sku sku of requested inventory entry
     * @param supplyChannel optional reference to supply channel of requested inventory entry
     * @return {@link Optional} which may contain inventory entry of {@code sku} and {@code supplyChannel}
     */
    static Optional<InventoryEntry> getInventoryEntryBySkuAndSupplyChannel(@Nonnull final BlockingSphereClient
                                                                               sphereClient,
                                                                           @Nonnull final String sku,
                                                                           @Nullable final Reference<Channel>
                                                                               supplyChannel) {
        InventoryEntryQuery query = InventoryEntryQuery.of()
            .plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.sku().is(sku));
        query = supplyChannel == null
            ? query.plusPredicates(
                inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel().isNotPresent())
            : query.plusPredicates(
                inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel().is(supplyChannel));
        return sphereClient.executeBlocking(query).head();
    }

    private static InventoryEntryQuery inventoryEntryQuerySupplier() {
        return InventoryEntryQuery.of().withLimit(QUERY_MAX_LIMIT);
    }

    private static ChannelQuery supplyChannelQuerySupplier() {
        return ChannelQuery.of()
            .withLimit(QUERY_MAX_LIMIT)
            .plusPredicates(channelQueryModel ->
                channelQueryModel.roles()
                    .containsAny(singleton(ChannelRole.INVENTORY_SUPPLY)));
    }
}
