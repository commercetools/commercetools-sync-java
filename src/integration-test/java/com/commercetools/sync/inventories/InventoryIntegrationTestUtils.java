package com.commercetools.sync.inventories;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.commons.utils.SphereClientUtils.fetchAndProcess;
import static java.util.Collections.singleton;

class InventoryIntegrationTestUtils {

    static final String SKU_1 = "100000";
    static final String SKU_2 = "200000";

    static final Long QUANTITY_ON_STOCK_1 = 1L;
    static final Long QUANTITY_ON_STOCK_2 = 2L;

    static final Integer RESTOCKABLE_IN_DAYS_1 = 1;
    static final Integer RESTOCKABLE_IN_DAYS_2 = 2;

    static final ZonedDateTime EXPECTED_DELIVERY_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    static final ZonedDateTime EXPECTED_DELIVERY_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    static final String SUPPLY_CHANNEL_KEY_1 = "channel-key_1";
    static final String SUPPLY_CHANNEL_KEY_2 = "channel-key_2";

    /**
     * Deletes up to {@link com.commercetools.sync.commons.utils.SphereClientUtils#QUERY_MAX_LIMIT} inventory entries
     * from CTP project, represented by provided {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     */
    static void deleteInventoryEntries(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, InventoryIntegrationTestUtils::inventoryEntryQuerySupplier,
            InventoryEntryDeleteCommand::of);
    }

    /**
     * Deletes up to {@link com.commercetools.sync.commons.utils.SphereClientUtils#QUERY_MAX_LIMIT} channels containing
     * {@link ChannelRole#INVENTORY_SUPPLY} role from CTP project, represented by provided {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     */
    static void deleteSupplyChannels(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, InventoryIntegrationTestUtils::supplyChannelQuerySupplier,
            ChannelDeleteCommand::of);
    }

    /**
     * Deletes inventory entries and supply channels from both source and target projects.
     */
    static void deleteInventoriesAndSupplyChannels() {
        deleteInventoryEntries(CTP_SOURCE_CLIENT);
        deleteSupplyChannels(CTP_SOURCE_CLIENT);
        deleteInventoryEntries(CTP_TARGET_CLIENT);
        deleteSupplyChannels(CTP_TARGET_CLIENT);
    }

    /**
     * Populate source CTP project.
     * Creates supply channel of key SUPPLY_CHANNEL_KEY_1.
     * Creates supply channel of key SUPPLY_CHANNEL_KEY_2.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2 and
     * reference to firstly created supply channel.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2 and
     * reference to secondly created supply channel.
     */
    static void populateSourceProject() {
        final ChannelDraft channelDraft1 = ChannelDraft.of(SUPPLY_CHANNEL_KEY_1)
            .withRoles(ChannelRole.INVENTORY_SUPPLY);
        final ChannelDraft channelDraft2 = ChannelDraft.of(SUPPLY_CHANNEL_KEY_2)
            .withRoles(ChannelRole.INVENTORY_SUPPLY);

        final String channelId1 = CTP_SOURCE_CLIENT.execute(ChannelCreateCommand.of(channelDraft1))
            .toCompletableFuture().join().getId();
        final String channelId2 = CTP_SOURCE_CLIENT.execute(ChannelCreateCommand.of(channelDraft2))
            .toCompletableFuture().join().getId();

        final Reference<Channel> supplyChannelReference1 = Channel.referenceOfId(channelId1);
        final Reference<Channel> supplyChannelReference2 = Channel.referenceOfId(channelId2);

        final InventoryEntryDraft draft1 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_1,
            EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, null).build();
        final InventoryEntryDraft draft2 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_2,
            EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference1).build();
        final InventoryEntryDraft draft3 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_2,
            EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference2).build();

        CTP_SOURCE_CLIENT.execute(InventoryEntryCreateCommand.of(draft1)).toCompletableFuture().join();
        CTP_SOURCE_CLIENT.execute(InventoryEntryCreateCommand.of(draft2)).toCompletableFuture().join();
        CTP_SOURCE_CLIENT.execute(InventoryEntryCreateCommand.of(draft3)).toCompletableFuture().join();

    }

    /**
     * Populate target CTP project.
     * Creates supply channel of key SUPPLY_CHANNEL_KEY_1.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1.
     * Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1 and
     * reference to supply channel created before.
     */
    static void populateTargetProject() {
        final ChannelDraft channelDraft = ChannelDraft.of(SUPPLY_CHANNEL_KEY_1).withRoles(ChannelRole.INVENTORY_SUPPLY);
        final String channelId = CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(channelDraft))
            .toCompletableFuture().join().getId();
        final Reference<Channel> supplyChannelReference = Channel.referenceOfId(channelId);

        final InventoryEntryDraft draft1 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_1,
            EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, null).build();
        final InventoryEntryDraft draft2 = InventoryEntryDraftBuilder.of(SKU_1, QUANTITY_ON_STOCK_1,
            EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, supplyChannelReference).build();

        CTP_TARGET_CLIENT.execute(InventoryEntryCreateCommand.of(draft1)).toCompletableFuture().join();
        CTP_TARGET_CLIENT.execute(InventoryEntryCreateCommand.of(draft2)).toCompletableFuture().join();
    }

    /**
     * Tries to fetch inventory entry of {@code sku} and {@code supplyChannel} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     * @param sku sku of requested inventory entry
     * @param supplyChannel optional reference to supply channel of requested inventory entry
     * @return {@link Optional} which may contain inventory entry of {@code sku} and {@code supplyChannel}
     */
    static Optional<InventoryEntry> getInventoryEntryBySkuAndSupplyChannel(@Nonnull final SphereClient sphereClient,
                                                                           @Nonnull final String sku,
                                                                           @Nullable final Reference<Channel>
                                                                               supplyChannel) {
        InventoryEntryQuery query = InventoryEntryQuery.of().plusPredicates(inventoryEntryQueryModel ->
            inventoryEntryQueryModel.sku().is(sku));
        query = supplyChannel == null
            ? query.plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel().isNotPresent())
            : query.plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel()
            .is(supplyChannel));
        return sphereClient.execute(query).toCompletableFuture().join().head();
    }

    private static InventoryEntryQuery inventoryEntryQuerySupplier() {
        return InventoryEntryQuery.of().withLimit(QUERY_MAX_LIMIT);
    }

    private static ChannelQuery supplyChannelQuerySupplier() {
        return ChannelQuery.of().withLimit(QUERY_MAX_LIMIT).plusPredicates(channelQueryModel ->
            channelQueryModel.roles().containsAny(singleton(ChannelRole.INVENTORY_SUPPLY)));
    }
}
