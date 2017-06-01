package com.commercetools.sync.inventories;

import com.commercetools.sync.commons.exceptions.ReferenceResolutionException;
import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.services.ChannelService;
import com.commercetools.sync.services.InventoryService;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.commercetools.sync.commons.MockUtils.getMockCustomFieldsDraft;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getCompletionStageWithException;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncTest {

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

    private static final Integer RESTOCKABLE_1 = 10;
    private static final Integer RESTOCKABLE_2 = 10;

    private static final ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private static final ZonedDateTime DATE_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private List<InventoryEntryDraft> drafts;
    private List<InventoryEntry> existingInventories;
    private List<String> errorCallBackMessages;
    private List<Throwable> errorCallBackExceptions;

    /**
     * Initialises test data.
     */
    @Before
    public void setup() {
        final Channel channel1 = getMockSupplyChannel(REF_1, KEY_1);
        final Channel channel2 = getMockSupplyChannel(REF_2, KEY_2);

        final Reference<Channel> reference1 = Channel.referenceOfId(REF_1).filled(channel1);
        final Reference<Channel> reference2 = Channel.referenceOfId(REF_2).filled(channel2);

        existingInventories = asList(
                getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null),
                getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference1, null),
                getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference2, null),
                getMockInventoryEntry(SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null),
                getMockInventoryEntry(SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference1, null),
                getMockInventoryEntry(SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1, reference2, null)
        );

        drafts = asList(
                InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1,null),
                InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, reference1),
                InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_2)),
                InventoryEntryDraft.of(SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2,null),
                InventoryEntryDraft.of(SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2, reference1),
                InventoryEntryDraft.of(SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2, Channel.referenceOfId(KEY_2)),
                InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,null),
                InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1, reference1),
                InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_2))
        );

        errorCallBackMessages = new ArrayList<>();
        errorCallBackExceptions = new ArrayList<>();
    }

    @Test
    public void getStatistics_ShouldReturnProperStatistics() {
        final InventorySync inventorySync = getInventorySync(30, false);
        inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(9);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(3);
        assertThat(stats.getUpdated()).isEqualTo(4);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_ShouldReturnProperStatistics() {
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(9);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(3);
        assertThat(stats.getUpdated()).isEqualTo(4);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithEmptyList_ShouldNotSync() {
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(emptyList())
                .toCompletableFuture()
                .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(0);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithEnsuredChannels_ShouldCreateEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = getInventorySync(30, true);
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithNewChannel))
                .toCompletableFuture()
                .join();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithNotEnsuredChannels_ShouldNotSyncEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));

        final InventorySyncOptions options = getInventorySyncOptions(30, false, true);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.empty()));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithNewChannel))
                .toCompletableFuture()
                .join();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to resolve reference on InventoryEntryDraft with"
            + " sku:'3000'. Reason: Failed to resolve supply channel reference. Reason: Channel with key"
            + " 'channel-key_3' does not exist.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithDraftsWithNullSku_ShouldNotSync() {
        final InventoryEntryDraft draftWithNullSku = InventoryEntryDraft.of(null, 12);
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithNullSku))
                .toCompletableFuture()
                .join();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to process inventory entry without sku.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithDraftsWithEmptySku_ShouldNotSync() {
        final InventoryEntryDraft draftWithEmptySku = InventoryEntryDraft.of("", 12);
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithEmptySku))
                .toCompletableFuture()
                .join();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to process inventory entry without sku.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithExceptionWhenFetchingExistingInventoriesBatch_ShouldProcessThatBatch() {
        final InventorySyncOptions options = getInventorySyncOptions(1, false, true);

        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3), REF_2);
        when(inventoryService.fetchInventoryEntriesBySkus(singleton(SKU_1)))
            .thenReturn(getCompletionStageWithException());

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();

        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(9);
        assertThat(stats.getFailed()).isEqualTo(3);
        assertThat(stats.getCreated()).isEqualTo(3);
        assertThat(stats.getUpdated()).isEqualTo(3);
        assertThat(errorCallBackMessages).hasSize(3);
        assertThat(errorCallBackExceptions).hasSize(3);
    }

    @Test
    public void sync_WithExceptionWhenCreatingOrUpdatingEntries_ShouldNotSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false, true);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(inventoryService.createInventoryEntry(any())).thenReturn(getCompletionStageWithException());
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(getCompletionStageWithException());

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.of(REF_2)));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(9);
        assertThat(stats.getFailed()).isEqualTo(7);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).hasSize(7);
        assertThat(errorCallBackExceptions).hasSize(7);
    }

    @Test
    public void sync_WithExistingInventoryEntryButWithNullCustomTypeReference_ShouldFailSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false, true);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.of(REF_2)));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final List<InventoryEntryDraft> newDrafts = new ArrayList<>();
        final InventoryEntryDraft draftWithNullCustomTypeId =
            InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, null)
                               .withCustom(getMockCustomFieldsDraft(null, new HashMap<>()));
        newDrafts.add(draftWithNullCustomTypeId);

        inventorySync.sync(newDrafts);
        assertThat(inventorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 inventory entries were processed in total "
                + "(0 created, 0 updated and 1 failed to sync).");
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).contains("Failed to resolve reference on InventoryEntryDraft with sku:'1000'."
            + " Reason: Failed to resolve custom type reference. Reason: Reference 'id' field value is blank "
            + "(null/empty).");
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithExistingInventoryEntryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false, true);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.of(REF_2)));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final List<InventoryEntryDraft> newDrafts = new ArrayList<>();
        final InventoryEntryDraft draftWithNullCustomTypeId =
            InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, null)
                               .withCustom(getMockCustomFieldsDraft("", new HashMap<>()));
        newDrafts.add(draftWithNullCustomTypeId);

        inventorySync.sync(newDrafts);
        assertThat(inventorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 inventory entries were processed in total "
                + "(0 created, 0 updated and 1 failed to sync).");
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).contains("Failed to resolve reference on InventoryEntryDraft with sku:'1000'."
            + " Reason: Failed to resolve custom type reference. Reason: Reference 'id' field value is blank "
            + "(null/empty).");
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithNotAllowedUuidCustomTypeKey_ShouldFailSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false, false);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.of(REF_2)));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        final List<InventoryEntryDraft> newDrafts = new ArrayList<>();
        final InventoryEntryDraft draftWithNullCustomTypeId =
            InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, null)
                               .withCustom(getMockCustomFieldsDraft(uuidCustomTypeKey, new HashMap<>()));
        newDrafts.add(draftWithNullCustomTypeId);

        inventorySync.sync(newDrafts);
        assertThat(inventorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getFailed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getUpdated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 inventory entries were processed in total "
                + "(0 created, 0 updated and 1 failed to sync).");
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).contains("Failed to resolve reference on InventoryEntryDraft with sku:'1000'."
            + " Reason: Failed to resolve custom type reference. Reason: Found a UUID in the id field. Expecting a key"
            + " without a UUID value. If you want to allow UUID values for reference keys, please use the "
            + "setAllowUuid(true) option in the sync options.");
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithAllowedUuidCustomTypeKey_ShouldSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false, true);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(completedFuture(existingInventories));

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.of(REF_2)));

        final TypeService mockTypeService = mock(TypeService.class);
        when(mockTypeService.fetchCachedTypeId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of("key")));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mockTypeService);

        final String uuidCustomTypeKey = UUID.randomUUID().toString();
        final List<InventoryEntryDraft> newDrafts = new ArrayList<>();
        final InventoryEntryDraft draftWithNullCustomTypeId =
            InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, null)
                               .withCustom(getMockCustomFieldsDraft(uuidCustomTypeKey, new HashMap<>()));
        newDrafts.add(draftWithNullCustomTypeId);

        inventorySync.sync(newDrafts);
        assertThat(inventorySync.getStatistics().getCreated()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getFailed()).isEqualTo(0);
        assertThat(inventorySync.getStatistics().getUpdated()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getProcessed()).isEqualTo(1);
        assertThat(inventorySync.getStatistics().getReportMessage()).isEqualTo(
            "Summary: 1 inventory entries were processed in total "
                + "(0 created, 1 updated and 0 failed to sync).");
    }

    @Test
    public void syncDrafts_WithExceptionWhenCreatingNewSupplyChannel_ShouldTriggerErrorCallbackAndIncrementFailed() {
        final InventorySyncOptions options = getInventorySyncOptions(3, true, false);

        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3), REF_2);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(channelService.createAndCacheChannel(anyString(), any())).thenReturn(getCompletionStageWithException());

        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraft
            .of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to create new supply channel of key '%s'.",
            KEY_3));
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void syncDrafts_WithNullInInputList_ShouldIncrementFailedStatistics() {
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3), REF_2);
        final InventorySyncOptions options = getInventorySyncOptions(3, false, false);

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(null))
            .toCompletableFuture()
            .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to process null inventory draft.");
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void syncDrafts_WithChannelNotPresentInMap_ShouldIncrementFailedStatistics() {
        final InventorySyncOptions options = getInventorySyncOptions(30, false, false);
        final Channel oldChannel = getMockSupplyChannel(REF_1, KEY_1);
        final Reference<Channel> oldChannelReference = Channel.referenceOfId(REF_1).filled(oldChannel);
        final List<InventoryEntry> oldInventories = singletonList(
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, oldChannelReference, null));

        final InventoryService inventoryService = getMockInventoryService(oldInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_2, KEY_2), REF_2);
        when(channelService.fetchCachedChannelIdByKeyAndRoles(anyString(), any()))
            .thenReturn(completedFuture(Optional.empty()));

        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraft
            .of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_1));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to resolve reference on InventoryEntryDraft with"
            + " sku:'1000'. Reason: Failed to resolve supply channel reference. Reason: Channel with key"
            + " 'channel-key_1' does not exist.");
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    private InventorySync getInventorySync(int batchSize, boolean ensureChannels) {
        final InventorySyncOptions options = getInventorySyncOptions(batchSize, ensureChannels, true);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3), REF_2);
        return new InventorySync(options, inventoryService, channelService, mock(TypeService.class));
    }

    private InventorySyncOptions getInventorySyncOptions(int batchSize, boolean ensureChannels, boolean allowUuid) {
        return InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                          .setBatchSize(batchSize)
                                          .ensureChannels(ensureChannels)
                                          .setAllowUuid(allowUuid)
                                          .setErrorCallBack((callBackError, exception) -> {
                                              errorCallBackMessages.add(callBackError);
                                              errorCallBackExceptions.add(exception);
                                          })
                                          .build();
    }
}
