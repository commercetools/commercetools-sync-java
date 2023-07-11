package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.api.client.ByProjectKeyInventoryGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.ChannelDraft;
import com.commercetools.api.models.channel.ChannelDraftBuilder;
import com.commercetools.api.models.channel.ChannelReference;
import com.commercetools.api.models.channel.ChannelResourceIdentifier;
import com.commercetools.api.models.channel.ChannelResourceIdentifierBuilder;
import com.commercetools.api.models.channel.ChannelRoleEnum;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.FieldDefinition;
import com.commercetools.api.models.type.FieldDefinitionBuilder;
import com.commercetools.api.models.type.FieldTypeBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.api.models.type.TypeDraft;
import com.commercetools.api.models.type.TypeDraftBuilder;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

public final class InventoryITUtils {

  public static final String SKU_1 = "100000";
  public static final String SKU_2 = "200000";

  public static final Long QUANTITY_ON_STOCK_1 = 1L;
  public static final Long QUANTITY_ON_STOCK_2 = 2L;

  public static final Long RESTOCKABLE_IN_DAYS_1 = 1L;
  public static final Long RESTOCKABLE_IN_DAYS_2 = 2L;

  public static final ZonedDateTime EXPECTED_DELIVERY_1 =
      ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
  public static final ZonedDateTime EXPECTED_DELIVERY_2 =
      ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

  public static final String SUPPLY_CHANNEL_KEY_1 = "channel-key_1";
  public static final String SUPPLY_CHANNEL_KEY_2 = "channel-key_2";

  public static final String CUSTOM_TYPE = "inventory-custom-type-name";
  public static final String CUSTOM_FIELD_NAME = "backgroundColor";
  public static final ReferenceIdToKeyCache REFERENCE_ID_TO_KEY_CACHE =
      new CaffeineReferenceIdToKeyCacheImpl();
  private static final InventoryEntryDraft INVENTORY_ENTRY_DRAFT_1 = InventoryEntryDraftBuilder.of()
      .sku(SKU_1)
      .quantityOnStock(QUANTITY_ON_STOCK_1)
      .expectedDelivery(EXPECTED_DELIVERY_1)
      .restockableInDays(RESTOCKABLE_IN_DAYS_1)
      .supplyChannel((ChannelResourceIdentifier) null)
      .custom(
          CustomFieldsDraftBuilder.of()
              .type(
                  typeResourceIdentifierBuilder ->
                      typeResourceIdentifierBuilder.key(CUSTOM_TYPE))
              .fields(createCustomFieldsJsonMap())
              .build())
      .build();
  private static final InventoryEntryDraft INVENTORY_ENTRY_DRAFT_2 = InventoryEntryDraftBuilder.of()
          .sku(SKU_1)
        .quantityOnStock(QUANTITY_ON_STOCK_2)
        .expectedDelivery(EXPECTED_DELIVERY_2)
        .restockableInDays(RESTOCKABLE_IN_DAYS_2)
        .custom(
          CustomFieldsDraftBuilder.of()
                .type(
          typeResourceIdentifierBuilder ->
          typeResourceIdentifierBuilder.key(CUSTOM_TYPE))
          .fields(createCustomFieldsJsonMap())
          .build())
          .build();

  /**
   * Deletes all inventory entries from CTP project, represented by provided {@code ctpClient}.
   *
   * @param ctpClient represents the CTP project the inventory entries will be deleted from.
   */
  public static void deleteInventoryEntries(@Nonnull final ProjectApiRoot ctpClient) {
    QueryUtils.queryAll(
            ctpClient.inventory().get(),
            inventoryEntries -> {
              CompletableFuture.allOf(
                      inventoryEntries.stream()
                          .map(inventoryEntry -> deleteInventoryEntry(ctpClient, inventoryEntry))
                          .map(CompletionStage::toCompletableFuture)
                          .toArray(CompletableFuture[]::new))
                  .join();
            })
        .toCompletableFuture()
        .join();
  }

  private static CompletionStage<InventoryEntry> deleteInventoryEntry(
      ProjectApiRoot ctpClient, InventoryEntry inventoryEntry) {
    return ctpClient
        .inventory()
        .delete(inventoryEntry)
        .execute()
        .thenApply(ApiHttpResponse::getBody);
  }

  /**
   * Deletes all inventory entries from CTP projects defined by {@code CTP_SOURCE_CLIENT} and {@code
   * CTP_TARGET_CLIENT}.
   */
  public static void deleteInventoryEntriesFromTargetAndSource() {
    deleteInventoryEntries(CTP_SOURCE_CLIENT);
    deleteInventoryEntries(CTP_TARGET_CLIENT);
  }

  /**
   * Populate source CTP project. Creates supply channel of key SUPPLY_CHANNEL_KEY_1. Creates supply
   * channel of key SUPPLY_CHANNEL_KEY_2. Creates inventory entry of values: SKU_1,
   * QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1. Creates inventory entry of
   * values: SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2 and reference to
   * firstly created supply channel. Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_2,
   * EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2 and reference to secondly created supply channel.
   */
  public static void populateSourceProject() {
    final ChannelDraft channelDraft1 =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_1)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();
    final ChannelDraft channelDraft2 =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_2)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();

    final String channelId1 =
        CTP_SOURCE_CLIENT.channels().create(channelDraft1).execute().join().getBody().getId();
    final String channelId2 =
        CTP_SOURCE_CLIENT.channels().create(channelDraft2).execute().join().getBody().getId();

    final ChannelResourceIdentifier supplyChannelReference1 =
        ChannelResourceIdentifierBuilder.of().id(channelId1).build();
    final ChannelResourceIdentifier supplyChannelReference2 =
        ChannelResourceIdentifierBuilder.of().id(channelId2).build();

    ensureInventoriesCustomType(CTP_SOURCE_CLIENT);

    final InventoryEntryDraft draft2 =
            InventoryEntryDraftBuilder.of(INVENTORY_ENTRY_DRAFT_2)
            .supplyChannel(supplyChannelReference1)
            .build();

    final InventoryEntryDraft draft3 =
            InventoryEntryDraftBuilder.of(INVENTORY_ENTRY_DRAFT_2)
            .supplyChannel(supplyChannelReference2)
            .build();

    CTP_SOURCE_CLIENT.inventory().create(INVENTORY_ENTRY_DRAFT_1).execute().toCompletableFuture().join();
    CTP_SOURCE_CLIENT.inventory().create(draft2).execute().toCompletableFuture().join();
    CTP_SOURCE_CLIENT.inventory().create(draft3).execute().toCompletableFuture().join();
  }

  /**
   * Populate target CTP project. Creates supply channel of key SUPPLY_CHANNEL_KEY_1. Creates
   * inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1,
   * RESTOCKABLE_IN_DAYS_1. Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1,
   * EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1 and reference to supply channel created before.
   * Creates inventory custom type of key CUSTOM_TYPE, and String field definition of name
   * CUSTOM_FIELD_NAME.
   */
  public static void populateTargetProject() {
    final ChannelDraft channelDraft =
        ChannelDraftBuilder.of()
            .key(SUPPLY_CHANNEL_KEY_1)
            .roles(ChannelRoleEnum.INVENTORY_SUPPLY)
            .build();

    final String channelId =
        CTP_TARGET_CLIENT
            .channels()
            .create(channelDraft)
            .execute()
            .thenApply(ApiHttpResponse::getBody)
            .toCompletableFuture()
            .join()
            .getId();
    final ChannelResourceIdentifier supplyChannelReference =
        ChannelResourceIdentifierBuilder.of().id(channelId).build();

    ensureInventoriesCustomType(CTP_TARGET_CLIENT);

    final InventoryEntryDraft draft2 =
        InventoryEntryDraftBuilder.of(INVENTORY_ENTRY_DRAFT_1)
            .supplyChannel(supplyChannelReference)
            .build();

    CTP_TARGET_CLIENT.inventory().create(INVENTORY_ENTRY_DRAFT_1).execute().toCompletableFuture().join();
    CTP_TARGET_CLIENT.inventory().create(draft2).execute().toCompletableFuture().join();
  }

  public static Type ensureInventoriesCustomType(@Nonnull final ProjectApiRoot ctpClient) {
    return createTypeIfNotAlreadyExisting(
            CUSTOM_TYPE,
            Locale.ENGLISH,
            CUSTOM_TYPE,
            Collections.singletonList(ResourceTypeId.INVENTORY_ENTRY),
            ctpClient);
  }

  private static Map<String, JsonNode> getMockCustomFieldsJsons() {
    final Map<String, JsonNode> customFieldsJsons = new HashMap<>();
    customFieldsJsons.put(CUSTOM_FIELD_NAME, JsonNodeFactory.instance.textNode("customValue"));
    return customFieldsJsons;
  }

  /**
   * Tries to fetch inventory entry of {@code sku} and {@code supplyChannel} using {@code
   * ctpClient}.
   *
   * @param ctpClient sphere client used to execute requests
   * @param sku sku of requested inventory entry
   * @param supplyChannel optional reference to supply channel of requested inventory entry
   * @param expand
   * @return {@link java.util.Optional} which may contain inventory entry of {@code sku} and {@code
   *     supplyChannel}
   */
  public static Optional<InventoryEntry> getInventoryEntryBySkuAndSupplyChannel(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nonnull final String sku,
      @Nullable final ChannelReference supplyChannel,
      @Nullable final String expand) {
    ByProjectKeyInventoryGet query =
        ctpClient.inventory().get().withWhere("sku=:sku").withPredicateVar("sku", sku);

    query = StringUtils.isBlank(expand) ? query : query.withExpand(expand);

    query =
        supplyChannel == null
            ? query.addWhere("supplyChannel is not defined")
            : query.addWhere("supplyChannel(id=:id)").addPredicateVar("id", supplyChannel.getId());
    return query
        .execute()
        .thenApply(ApiHttpResponse::getBody)
        .toCompletableFuture()
        .join()
        .getResults()
        .stream()
        .findFirst();
  }

  private InventoryITUtils() {}
}
