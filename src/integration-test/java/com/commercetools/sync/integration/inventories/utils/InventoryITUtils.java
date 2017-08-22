package com.commercetools.sync.integration.inventories.utils;

import com.commercetools.sync.integration.commons.utils.SphereClientUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.channels.ChannelDraft;
import io.sphere.sdk.channels.ChannelRole;
import io.sphere.sdk.channels.commands.ChannelCreateCommand;
import io.sphere.sdk.channels.commands.ChannelDeleteCommand;
import io.sphere.sdk.channels.queries.ChannelQuery;
import io.sphere.sdk.channels.queries.ChannelQueryBuilder;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.inventory.commands.InventoryEntryCreateCommand;
import io.sphere.sdk.inventory.commands.InventoryEntryDeleteCommand;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.LocalizedString;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.FieldDefinition;
import io.sphere.sdk.types.StringFieldType;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.TypeDraft;
import io.sphere.sdk.types.TypeDraftBuilder;
import io.sphere.sdk.types.commands.TypeCreateCommand;
import io.sphere.sdk.types.queries.TypeQuery;
import io.sphere.sdk.types.queries.TypeQueryBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.CTP_TARGET_CLIENT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.QUERY_MAX_LIMIT;
import static com.commercetools.sync.integration.commons.utils.SphereClientUtils.fetchAndProcess;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class InventoryITUtils {

    public static final String SKU_1 = "100000";
    public static final String SKU_2 = "200000";

    public static final Long QUANTITY_ON_STOCK_1 = 1L;
    public static final Long QUANTITY_ON_STOCK_2 = 2L;

    public static final Integer RESTOCKABLE_IN_DAYS_1 = 1;
    public static final Integer RESTOCKABLE_IN_DAYS_2 = 2;

    public static final ZonedDateTime EXPECTED_DELIVERY_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    public static final ZonedDateTime EXPECTED_DELIVERY_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    public static final String SUPPLY_CHANNEL_KEY_1 = "channel-key_1";
    public static final String SUPPLY_CHANNEL_KEY_2 = "channel-key_2";

    public static final String CUSTOM_TYPE = "inventory-custom-type-name";
    public static final String CUSTOM_FIELD_NAME = "inventory-custom-field-1";

    private static final InventoryEntryQuery QUERY_ALL_INVENTORIES = InventoryEntryQuery.of()
        .withLimit(QUERY_MAX_LIMIT);

    private static final ChannelQuery QUERY_ALL_SUPPLY_CHANNELS = ChannelQuery.of().withLimit(QUERY_MAX_LIMIT)
        .plusPredicates(queryModel -> queryModel.roles().containsAny(singleton(ChannelRole.INVENTORY_SUPPLY)));

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} inventory entries
     * from CTP project, represented by provided {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     */
    public static void deleteInventoryEntries(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, QUERY_ALL_INVENTORIES, InventoryEntryDeleteCommand::of);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} channels containing
     * {@link ChannelRole#INVENTORY_SUPPLY} role from CTP project, represented by provided {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     */
    public static void deleteSupplyChannels(@Nonnull final SphereClient sphereClient) {
        fetchAndProcess(sphereClient, QUERY_ALL_SUPPLY_CHANNELS, ChannelDeleteCommand::of);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} inventory entries
     * from CTP projects {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteInventoryEntriesFromTargetAndSource() {
        deleteInventoryEntries(CTP_SOURCE_CLIENT);
        deleteInventoryEntries(CTP_TARGET_CLIENT);
    }

    /**
     * Deletes up to {@link SphereClientUtils#QUERY_MAX_LIMIT} supply channels
     * from CTP projects {@code CTP_SOURCE_CLIENT} and {@code CTP_TARGET_CLIENT}.
     */
    public static void deleteChannelsFromTargetAndSource() {
        deleteSupplyChannels(CTP_SOURCE_CLIENT);
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
    public static void populateSourceProject() {
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

        createInventoriesCustomType(CTP_SOURCE_CLIENT);

        final InventoryEntryDraft draft1 = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, null)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons())).build();
        final InventoryEntryDraft draft2 = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference1)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons())).build();
        final InventoryEntryDraft draft3 = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2, supplyChannelReference2)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons())).build();

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
     * Creates inventory custom type of key CUSTOM_TYPE, and String field definition of name CUSTOM_FIELD_NAME.
     */
    public static void populateTargetProject() {
        final ChannelDraft channelDraft = ChannelDraft.of(SUPPLY_CHANNEL_KEY_1).withRoles(ChannelRole.INVENTORY_SUPPLY);
        final String channelId = CTP_TARGET_CLIENT.execute(ChannelCreateCommand.of(channelDraft))
            .toCompletableFuture().join().getId();
        final Reference<Channel> supplyChannelReference = Channel.referenceOfId(channelId);

        createInventoriesCustomType(CTP_TARGET_CLIENT);

        final InventoryEntryDraft draft1 = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, null)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons()))
            .build();
        final InventoryEntryDraft draft2 = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1, supplyChannelReference)
            .custom(CustomFieldsDraft.ofTypeKeyAndJson(CUSTOM_TYPE, getMockCustomFieldsJsons()))
            .build();

        CTP_TARGET_CLIENT.execute(InventoryEntryCreateCommand.of(draft1)).toCompletableFuture().join();
        CTP_TARGET_CLIENT.execute(InventoryEntryCreateCommand.of(draft2)).toCompletableFuture().join();
    }

    private static Type createInventoriesCustomType(@Nonnull final SphereClient ctpClient) {
        final FieldDefinition fieldDefinition = FieldDefinition
            .of(StringFieldType.of(), CUSTOM_FIELD_NAME, LocalizedString.of(Locale.ENGLISH, CUSTOM_FIELD_NAME), false);
        final TypeDraft typeDraft = TypeDraftBuilder.of(CUSTOM_TYPE, LocalizedString.of(Locale.ENGLISH, CUSTOM_TYPE),
            Collections.singleton(InventoryEntry.resourceTypeId())).fieldDefinitions(singletonList(fieldDefinition))
                                                    .build();
        return ctpClient.execute(TypeCreateCommand.of(typeDraft)).toCompletableFuture().join();
    }

    private static Map<String, JsonNode> getMockCustomFieldsJsons() {
        final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
        customFieldsJsons
            .put(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("customValue"));
        return customFieldsJsons;
    }

    /**
     * Tries to fetch inventory entry of {@code sku} and {@code supplyChannel} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     * @param sku sku of requested inventory entry
     * @param supplyChannel optional reference to supply channel of requested inventory entry
     * @return {@link Optional} which may contain inventory entry of {@code sku} and {@code supplyChannel}
     */
    public static Optional<InventoryEntry> getInventoryEntryBySkuAndSupplyChannel(@Nonnull final SphereClient
                                                                                      sphereClient,
                                                                                  @Nonnull final String sku,
                                                                                  @Nullable final Reference<Channel>
                                                                                      supplyChannel) {
        InventoryEntryQuery query = InventoryEntryQuery.of()
                                                       .withExpansionPaths(ExpansionPath.of("custom.type"))
                                                       .plusPredicates(inventoryEntryQueryModel ->
            inventoryEntryQueryModel.sku().is(sku));
        query = supplyChannel == null
            ? query.plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel().isNotPresent())
            : query.plusPredicates(inventoryEntryQueryModel -> inventoryEntryQueryModel.supplyChannel()
            .is(supplyChannel));
        return sphereClient.execute(query).toCompletableFuture().join().head();
    }

    /**
     * Tries to fetch channel of key {@code channelKey} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     * @param channelKey key of requested channel
     * @return {@link Optional} which may contain channel of key {@code channelKey}
     */
    public static Optional<Channel> getChannelByKey(@Nonnull final SphereClient sphereClient,
                                                    @Nonnull final String channelKey) {
        final ChannelQuery channelQuery = ChannelQueryBuilder.of().plusPredicates(channelQueryModel ->
            channelQueryModel.key().is(channelKey)).build();
        return sphereClient.execute(channelQuery).toCompletableFuture().join().head();
    }

    /**
     * Tries to fetch type of key {@code typeKey} using {@code sphereClient}.
     *
     * @param sphereClient sphere client used to execute requests
     * @param typeKey key of requested type
     * @return {@link Optional} which may contain type of key {@code typeKey}
     */
    public static Optional<Type> getTypeByKey(@Nonnull final SphereClient sphereClient, @Nonnull final String typeKey) {
        final TypeQuery typeQuery = TypeQueryBuilder.of().plusPredicates(typeQueryModel ->
            typeQueryModel.key().is(typeKey)).build();
        return sphereClient.execute(typeQuery).toCompletableFuture().join().head();
    }
}
