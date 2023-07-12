package com.commercetools.sync.integration.sdk2.commons.utils;

import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createCustomFieldsJsonMap;
import static com.commercetools.sync.integration.sdk2.commons.utils.ITUtils.createTypeIfNotAlreadyExisting;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_SOURCE_CLIENT;
import static com.commercetools.sync.integration.sdk2.commons.utils.TestClientUtils.CTP_TARGET_CLIENT;

import com.commercetools.api.client.ByProjectKeyInventoryGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.client.QueryUtils;
import com.commercetools.api.models.channel.*;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.type.CustomFieldsDraft;
import com.commercetools.api.models.type.CustomFieldsDraftBuilder;
import com.commercetools.api.models.type.ResourceTypeId;
import com.commercetools.api.models.type.Type;
import com.commercetools.sync.sdk2.commons.utils.CaffeineReferenceIdToKeyCacheImpl;
import com.commercetools.sync.sdk2.commons.utils.ReferenceIdToKeyCache;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vrap.rmf.base.client.ApiHttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
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

  public static final String CUSTOM_TYPE = "inventory-custom-type-name";
  public static final String CUSTOM_FIELD_NAME = "backgroundColor";
  public static final ObjectNode CUSTOM_FIELD_VALUE =
      JsonNodeFactory.instance.objectNode().put("en", "purple");
  public static final ReferenceIdToKeyCache REFERENCE_ID_TO_KEY_CACHE =
      new CaffeineReferenceIdToKeyCacheImpl();
  private static final InventoryEntryDraft INVENTORY_ENTRY_DRAFT_1 =
      InventoryEntryDraftBuilder.of()
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
  private static final InventoryEntryDraft INVENTORY_ENTRY_DRAFT_2 =
      InventoryEntryDraftBuilder.of()
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
   * Populate source CTP project.Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1,
   * EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1. Takes a list of Channels and creates inventory
   * entries of values: SKU_1, QUANTITY_ON_STOCK_2, EXPECTED_DELIVERY_2, RESTOCKABLE_IN_DAYS_2 and
   * reference to the given supply channel.
   *
   * @param supplyChannels
   */
  public static void populateInventoriesInSourceProject(List<Channel> supplyChannels) {
    Set<ChannelResourceIdentifier> channelResourceIdentifiers =
        supplyChannels.stream()
            .map(channel -> ChannelResourceIdentifierBuilder.of().id(channel.getId()).build())
            .collect(Collectors.toSet());

    final List<InventoryEntryDraft> draftsToCreate = new ArrayList<>();
    draftsToCreate.add(INVENTORY_ENTRY_DRAFT_1);

    channelResourceIdentifiers.forEach(
        supplyChannelReference ->
            draftsToCreate.add(
                InventoryEntryDraftBuilder.of(INVENTORY_ENTRY_DRAFT_2)
                    .supplyChannel(supplyChannelReference)
                    .build()));

    draftsToCreate.forEach(
        draft -> CTP_SOURCE_CLIENT.inventory().create(draft).execute().toCompletableFuture());
  }

  /**
   * Populate target CTP project. Creates inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1,
   * EXPECTED_DELIVERY_1, RESTOCKABLE_IN_DAYS_1. Takes a ChannelResourceIdentifier and creates
   * inventory entry of values: SKU_1, QUANTITY_ON_STOCK_1, EXPECTED_DELIVERY_1,
   * RESTOCKABLE_IN_DAYS_1 and reference to given channel.
   *
   * @param supplyChannelReference
   */
  public static void populateInventoriesInTargetProject(
      final ChannelResourceIdentifier supplyChannelReference) {

    final InventoryEntryDraft draft2 =
        InventoryEntryDraftBuilder.of(INVENTORY_ENTRY_DRAFT_1)
            .supplyChannel(supplyChannelReference)
            .build();

    CTP_TARGET_CLIENT
        .inventory()
        .create(INVENTORY_ENTRY_DRAFT_1)
        .execute()
        .toCompletableFuture()
        .join();
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

  public static CustomFieldsDraft getMockCustomFieldsDraft() {
    return CustomFieldsDraftBuilder.of()
        .type(typeResourceIdentifierBuilder -> typeResourceIdentifierBuilder.key(CUSTOM_TYPE))
        .fields(
            fieldContainerBuilder ->
                fieldContainerBuilder.addValue(CUSTOM_FIELD_NAME, CUSTOM_FIELD_VALUE))
        .build();
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
