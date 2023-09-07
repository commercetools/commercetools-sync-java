package com.commercetools.sync.inventories;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.channel.*;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryDraftBuilder;
import com.commercetools.api.models.type.*;
import com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics;
import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.commons.helpers.BaseReferenceResolver;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.TypeService;
import com.commercetools.sync.services.impl.TypeServiceImpl;
import io.vrap.rmf.base.client.ApiHttpResponse;
import io.vrap.rmf.base.client.error.BadRequestException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InventorySyncTest {

  private static final String SKU_1 = "1000";
  private static final String SKU_2 = "2000";
  private static final String SKU_3 = "3000";

  private static final String KEY_1 = "channel-key_1";
  private static final String KEY_2 = "channel-key_2";
  private static final String KEY_3 = "channel-key_3";

  private static final String REF_1 = "111";
  private static final String REF_2 = "222";
  private static final String REF_3 = "333";

  private static final Long QUANTITY_1 = 10L;
  private static final Long QUANTITY_2 = 30L;

  private static final Long RESTOCKABLE_1 = 10L;
  private static final Long RESTOCKABLE_2 = 10L;

  private static final ZonedDateTime DATE_1 =
      ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
  private static final ZonedDateTime DATE_2 =
      ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

  private List<InventoryEntryDraft> drafts;
  private Set<InventoryEntry> existingInventories;
  private List<String> errorCallBackMessages;
  private List<Throwable> errorCallBackExceptions;

  /** Initialises test data. */
  @BeforeEach
  void setup() {
    final ChannelReference reference1 = ChannelReferenceBuilder.of().id(REF_1).build();
    final ChannelReference reference2 = ChannelReferenceBuilder.of().id(REF_2).build();

    existingInventories =
        Set.of(
            InventorySyncMockUtils.getMockInventoryEntry(
                SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null),
            InventorySyncMockUtils.getMockInventoryEntry(
                SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference2, null),
            InventorySyncMockUtils.getMockInventoryEntry(
                SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null),
            InventorySyncMockUtils.getMockInventoryEntry(
                SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference1, null),
            InventorySyncMockUtils.getMockInventoryEntry(
                SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference2, null));

    drafts =
        List.of(
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1)
                .build(),
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1)
                .supplyChannel(resourceIdentifierBuilder -> resourceIdentifierBuilder.key(KEY_2))
                .build(),
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2)
                .build(),
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2)
                .supplyChannel(resourceIdentifierBuilder -> resourceIdentifierBuilder.key(KEY_1))
                .build(),
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2)
                .supplyChannel(resourceIdentifierBuilder -> resourceIdentifierBuilder.key(KEY_2))
                .build(),
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1)
                .build(),
            InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                    SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1)
                .supplyChannel(resourceIdentifierBuilder -> resourceIdentifierBuilder.key(KEY_2))
                .build());

    errorCallBackMessages = new ArrayList<>();
    errorCallBackExceptions = new ArrayList<>();
  }

  @Test
  void getStatistics_ShouldReturnProperStatistics() {
    // preparation
    final InventorySync inventorySync = getInventorySync(30, false);
    inventorySync.sync(drafts).toCompletableFuture().join();

    // test
    final InventorySyncStatistics stats = inventorySync.getStatistics();

    // assertion
    assertThat(stats).hasValues(7, 2, 3, 0);
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
  }

  @Test
  void sync_WithEmptyList_ShouldNotSync() {
    final InventorySync inventorySync = getInventorySync(30, false);
    final InventorySyncStatistics stats =
        inventorySync.sync(emptyList()).toCompletableFuture().join();

    assertThat(stats).hasValues(0, 0, 0, 0);
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
  }

  @Test
  void sync_WithEnsuredChannels_ShouldCreateEntriesWithUnknownChannels() {
    final InventoryEntryDraft draftWithNewChannel =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.key(KEY_3))
            .build();
    final InventorySync inventorySync = getInventorySync(30, true);
    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(draftWithNewChannel)).toCompletableFuture().join();

    assertThat(stats).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
  }

  @Test
  void sync_WithNoNewCreatedInventory_ShouldIncrementFailedStatic() {
    final InventoryEntryDraft draftWithNewChannel =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.key(KEY_3))
            .build();
    final InventorySyncOptions options = getInventorySyncOptions(30, true);
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, null, mock(InventoryEntry.class));
    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_2, KEY_2));

    InventorySync inventorySync =
        new InventorySync(
            options, inventoryService, channelService, Mockito.mock(TypeService.class));
    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(draftWithNewChannel)).toCompletableFuture().join();
    assertThat(errorCallBackMessages).hasSize(0);
    assertThat(errorCallBackExceptions).hasSize(0);
    assertThat(stats).hasValues(1, 0, 0, 1);
  }

  @Test
  void sync_WithNotEnsuredChannels_ShouldNotSyncEntriesWithUnknownChannels() {
    final InventoryEntryDraft draftWithNewChannel =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.key(KEY_3))
            .build();

    final InventorySyncOptions options = getInventorySyncOptions(30, false);
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));
    final ChannelService channelService = mock(ChannelService.class);
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(completedFuture(Optional.empty()));

    final InventorySync inventorySync =
        new InventorySync(options, inventoryService, channelService, mock(TypeService.class));

    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(draftWithNewChannel)).toCompletableFuture().join();

    assertThat(stats).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            format(
                "Failed to process the InventoryEntryDraft"
                    + " with SKU:'%s'. Reason: %s: Failed to resolve supply channel resource identifier on"
                    + " InventoryEntryDraft with SKU:'%s'. Reason: Channel with key '%s' does not exist.",
                SKU_3, ReferenceResolutionException.class.getCanonicalName(), SKU_3, KEY_3));
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(ReferenceResolutionException.class);
  }

  @Test
  void sync_WithDraftsWithNullSku_ShouldNotSync() {
    final InventoryEntryDraft draftWithNullSku = InventoryEntryDraft.of();
    final InventorySync inventorySync = getInventorySync(30, false);
    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(draftWithNullSku)).toCompletableFuture().join();

    assertThat(stats).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            "InventoryEntryDraft doesn't have a SKU."
                + " Please make sure all inventory entry drafts have SKUs.");
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
  }

  @Test
  void sync_WithDraftsWithEmptySku_ShouldNotSync() {
    final InventoryEntryDraft draftWithEmptySku =
        InventoryEntryDraftBuilder.of().sku("").quantityOnStock(12L).build();
    final InventorySync inventorySync = getInventorySync(30, false);
    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(draftWithEmptySku)).toCompletableFuture().join();

    assertThat(stats).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            "InventoryEntryDraft doesn't have a SKU. "
                + "Please make sure all inventory entry drafts have SKUs.");
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
  }

  @Test
  void sync_WithExceptionWhenCreatingOrUpdatingEntries_ShouldNotSync() {
    final InventorySyncOptions options = getInventorySyncOptions(3, false);
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));
    when(inventoryService.createInventoryEntry(any()))
        .thenReturn(InventorySyncMockUtils.getCompletionStageWithException());
    when(inventoryService.updateInventoryEntry(any(), any()))
        .thenReturn(InventorySyncMockUtils.getCompletionStageWithException());

    final InventorySync inventorySync =
        new InventorySync(
            options, inventoryService, createMockChannelService(), mock(TypeService.class));
    final InventorySyncStatistics stats = inventorySync.sync(drafts).toCompletableFuture().join();

    assertThat(stats).hasValues(7, 0, 0, 5);
    assertThat(errorCallBackMessages).hasSize(5);
    assertThat(errorCallBackExceptions).hasSize(5);
  }

  @Test
  void sync_WithExceptionWhenUpdatingEntries_ShouldNotSync() {
    final InventorySyncOptions options = getInventorySyncOptions(3, false);
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));
    when(inventoryService.updateInventoryEntry(any(), any()))
        .thenReturn(InventorySyncMockUtils.getCompletionStageWithException());

    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_2, KEY_2));

    final InventorySync inventorySync =
        new InventorySync(options, inventoryService, channelService, mock(TypeService.class));

    final InventoryEntryDraft inventoryEntryDraft =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_2, QUANTITY_2, DATE_1, RESTOCKABLE_1)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.key(KEY_2))
            .build();

    final InventorySyncStatistics stats =
        inventorySync
            .sync(Collections.singletonList(inventoryEntryDraft))
            .toCompletableFuture()
            .join();

    assertThat(stats).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).hasSize(1);
    assertThat(errorCallBackExceptions).hasSize(1);
    assertThat(errorCallBackMessages.get(0))
        .isEqualTo(
            format(
                "Failed to update inventory entry of SKU '%s' and supply channel id '%s'.",
                SKU_2, REF_2));
    assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(RuntimeException.class);
  }

  @Test
  void sync_WithExistingInventoryEntryButWithEmptyCustomTypeReference_ShouldFailSync() {
    final InventorySyncOptions options = getInventorySyncOptions(3, false);
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    final ChannelService channelService = mock(ChannelService.class);
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(completedFuture(Optional.of(REF_2)));
    final InventorySync inventorySync =
        new InventorySync(options, inventoryService, channelService, mock(TypeService.class));

    final List<InventoryEntryDraft> newDrafts = new ArrayList<>();
    final CustomFieldsDraft customFieldDraftWithEmptyCustomTypeReference =
        CustomFieldsDraftBuilder.of()
            .type(TypeResourceIdentifier.of())
            .fields(FieldContainerBuilder.of().values(emptyMap()).build())
            .build();
    final InventoryEntryDraft draftWithNullCustomTypeId =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1)
            .custom(customFieldDraftWithEmptyCustomTypeReference)
            .build();
    newDrafts.add(draftWithNullCustomTypeId);

    final InventorySyncStatistics syncStatistics =
        inventorySync.sync(newDrafts).toCompletableFuture().join();

    assertThat(syncStatistics).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
        .contains(
            String.format(
                "Failed to process the"
                    + " InventoryEntryDraft with SKU:'%s'. Reason: %s: Failed to resolve custom type resource identifier on "
                    + "InventoryEntryDraft with SKU:'1000'. Reason: %s",
                SKU_1,
                ReferenceResolutionException.class.getCanonicalName(),
                BaseReferenceResolver.BLANK_KEY_VALUE_ON_RESOURCE_IDENTIFIER));
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(ReferenceResolutionException.class);
  }

  @Test
  void sync_WithNewSupplyChannelAndEnsure_ShouldSync() {
    final InventorySyncOptions options = getInventorySyncOptions(3, true);

    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_3, KEY_3));
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final InventoryEntryDraft newInventoryDraft =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.key(KEY_3))
            .build();
    final InventorySync inventorySync =
        new InventorySync(options, inventoryService, channelService, mock(TypeService.class));

    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();
    assertThat(stats).hasValues(1, 1, 0, 0);
    assertThat(errorCallBackMessages).isEmpty();
    assertThat(errorCallBackExceptions).isEmpty();
  }

  @Test
  void
      sync_WithExceptionWhenCreatingNewSupplyChannel_ShouldTriggerErrorCallbackAndIncrementFailed() {
    final InventorySyncOptions options = getInventorySyncOptions(3, true);

    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_3, KEY_3));
    when(channelService.fetchCachedChannelId(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
    when(channelService.createAndCacheChannel(anyString()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    final InventoryEntryDraft newInventoryDraft =
        InventorySyncMockUtils.createInventoryDraftBuilderWithoutChannel(
                SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1)
            .supplyChannel(
                channelResourceIdentifierBuilder -> channelResourceIdentifierBuilder.key(KEY_3))
            .build();
    final InventorySync inventorySync =
        new InventorySync(options, inventoryService, channelService, mock(TypeService.class));

    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(newInventoryDraft)).toCompletableFuture().join();

    assertThat(stats).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0))
        .contains(
            format(
                "Failed to resolve supply channel resource identifier"
                    + " on InventoryEntryDraft with SKU:'%s'. Reason: Failed to create supply channel with key: '%s'",
                SKU_1, "channel-key_3"));
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
    assertThat(errorCallBackExceptions.get(0).getCause())
        .isExactlyInstanceOf(ReferenceResolutionException.class);
    assertThat(errorCallBackExceptions.get(0).getCause().getCause())
        .isExactlyInstanceOf(CompletionException.class);
  }

  @Test
  void sync_WithNullInInputList_ShouldIncrementFailedStatistics() {
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_3, KEY_3));
    final InventorySyncOptions options = getInventorySyncOptions(3, false);

    final InventorySync inventorySync =
        new InventorySync(options, inventoryService, channelService, mock(TypeService.class));

    final InventorySyncStatistics stats =
        inventorySync.sync(singletonList(null)).toCompletableFuture().join();

    assertThat(stats).hasValues(1, 0, 0, 1);
    assertThat(errorCallBackMessages).isNotEmpty();
    assertThat(errorCallBackMessages.get(0)).isEqualTo("InventoryEntryDraft is null.");
    assertThat(errorCallBackExceptions).isNotEmpty();
    assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
  }

  @Test
  void sync_WithOnlyDraftsToUpdate_ShouldOnlyCallBeforeUpdateCallback() {
    // preparation
    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of().sku(SKU_1).quantityOnStock(1L).build();
    final InventorySyncOptions optionsSpy = spy(getInventorySyncOptions(1, false));

    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_3, KEY_3));

    final InventorySync inventorySync =
        new InventorySync(optionsSpy, inventoryService, channelService, mock(TypeService.class));

    // test
    inventorySync.sync(singletonList(inventoryEntryDraft)).toCompletableFuture().join();

    // assertion
    verify(optionsSpy).applyBeforeUpdateCallback(any(), any(), any());
    verify(optionsSpy, never()).applyBeforeCreateCallback(any());
  }

  @Test
  void sync_WithOnlyDraftsToCreate_ShouldCallBeforeCreateCallback() {
    // preparation
    final InventoryEntryDraft inventoryEntryDraft =
        InventoryEntryDraftBuilder.of().sku("newSKU").quantityOnStock(1L).build();
    final InventorySyncOptions optionsSpy = spy(getInventorySyncOptions(1, false));

    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    final ChannelService channelService =
        InventorySyncMockUtils.getMockChannelService(
            InventorySyncMockUtils.getMockSupplyChannel(REF_3, KEY_3));

    final InventorySync inventorySync =
        new InventorySync(optionsSpy, inventoryService, channelService, mock(TypeService.class));

    // test
    inventorySync.sync(singletonList(inventoryEntryDraft)).toCompletableFuture().join();

    // assertion
    verify(optionsSpy).applyBeforeCreateCallback(any());
    verify(optionsSpy, never()).applyBeforeUpdateCallback(any(), any(), any());
  }

  private InventorySync getInventorySync(int batchSize, boolean ensureChannels) {
    final InventorySyncOptions options = getInventorySyncOptions(batchSize, ensureChannels);
    final InventoryService inventoryService =
        InventorySyncMockUtils.getMockInventoryService(
            existingInventories, mock(InventoryEntry.class), mock(InventoryEntry.class));

    return new InventorySync(
        options, inventoryService, createMockChannelService(), mock(TypeService.class));
  }

  private ChannelService createMockChannelService() {
    final ChannelService channelService = mock(ChannelService.class);
    when(channelService.fetchCachedChannelId(anyString()))
        .thenAnswer(
            invocation -> {
              Object argument = invocation.getArguments()[0];
              if (argument.equals(KEY_1)) {
                return completedFuture(Optional.of(REF_1));
              } else if (argument.equals(KEY_2)) {
                return completedFuture(Optional.of(REF_2));
              } else if (argument.equals(KEY_3)) {
                return completedFuture(Optional.of(REF_3));
              }

              return completedFuture(Optional.empty());
            });
    when(channelService.createAndCacheChannel(anyString()))
        .thenAnswer(
            invocation -> {
              Object argument = invocation.getArguments()[0];
              if (argument.equals(KEY_1)) {
                return completedFuture(
                    Optional.of(InventorySyncMockUtils.getMockSupplyChannel(REF_1, KEY_1)));
              } else if (argument.equals(KEY_2)) {
                return completedFuture(
                    Optional.of(InventorySyncMockUtils.getMockSupplyChannel(REF_2, KEY_2)));
              } else if (argument.equals(KEY_3)) {
                return completedFuture(
                    Optional.of(InventorySyncMockUtils.getMockSupplyChannel(REF_3, KEY_3)));
              }

              return completedFuture(Optional.empty());
            });
    return channelService;
  }

  private InventorySyncOptions getInventorySyncOptions(int batchSize, boolean ensureChannels) {
    return InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
        .batchSize(batchSize)
        .ensureChannels(ensureChannels)
        .errorCallback(
            (exception, oldResource, newResource, updateActions) -> {
              errorCallBackMessages.add(exception.getMessage());
              errorCallBackExceptions.add(exception.getCause());
            })
        .build();
  }

  @Test
  void sync_WithFailOnCachingKeysToIds_ShouldTriggerErrorCallbackAndReturnProperStats() {
    // preparation
    final List<String> errorMessages = new ArrayList<>();
    final List<Throwable> exceptions = new ArrayList<>();

    final InventorySyncOptions inventorySyncOptions =
        InventorySyncOptionsBuilder.of(mock(ProjectApiRoot.class))
            .errorCallback(
                (exception, oldResource, newResource, actions) -> {
                  errorMessages.add(exception.getMessage());
                  exceptions.add(exception.getCause());
                })
            .build();

    final TypeService typeService = Mockito.spy(new TypeServiceImpl(inventorySyncOptions));
    when(typeService.cacheKeysToIds(anySet()))
        .thenReturn(
            supplyAsync(
                () -> {
                  throw new BadRequestException(
                      500, "", null, "", new ApiHttpResponse<>(500, null, null));
                }));

    final InventorySync inventorySync =
        new InventorySync(
            inventorySyncOptions,
            mock(InventoryService.class),
            mock(ChannelService.class),
            typeService);

    final CustomFieldsDraft mockCustomFieldsDraft = mock(CustomFieldsDraft.class);
    final TypeResourceIdentifier mockTypeResourceIdentifier = mock(TypeResourceIdentifier.class);
    when(mockTypeResourceIdentifier.getKey()).thenReturn("typeKey");
    when(mockCustomFieldsDraft.getType()).thenReturn(mockTypeResourceIdentifier);
    final InventoryEntryDraft newInventoryDraftWithCustomType = mock(InventoryEntryDraft.class);
    when(newInventoryDraftWithCustomType.getSku()).thenReturn("sku");
    when(newInventoryDraftWithCustomType.getCustom()).thenReturn(mockCustomFieldsDraft);

    // test
    final InventorySyncStatistics inventorySyncStatistics =
        inventorySync
            .sync(singletonList(newInventoryDraftWithCustomType))
            .toCompletableFuture()
            .join();

    // assertions
    AssertionsForStatistics.assertThat(inventorySyncStatistics).hasValues(1, 0, 0, 1);

    assertThat(errorMessages)
        .hasSize(1)
        .singleElement(as(STRING))
        .contains("Failed to build a cache of keys to ids.");

    assertThat(exceptions)
        .hasSize(1)
        .singleElement(as(THROWABLE))
        .isExactlyInstanceOf(CompletionException.class)
        .hasCauseExactlyInstanceOf(BadRequestException.class);
  }
}
