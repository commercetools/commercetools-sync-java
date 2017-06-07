package com.commercetools.sync.inventories;

import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.ConcurrentModificationException;
import io.sphere.sdk.client.NotFoundException;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

import static com.commercetools.sync.inventories.InventorySync.ATTEMPTS_ON_409_LIMIT;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getCompletionStageWithException;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static io.sphere.sdk.utils.CompletableFutureUtils.exceptionallyCompletedFuture;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private List<Channel> existingSupplyChannels;

    /**
     * Initialises test data.
     */
    @Before
    public void setup() {
        final Channel channel1 = getMockSupplyChannel(REF_1, KEY_1);
        final Channel channel2 = getMockSupplyChannel(REF_2, KEY_2);

        final Reference<Channel> expandedReference1 = Channel.referenceOfId(REF_1).filled(channel1);

        final Reference<Channel> reference1 = Channel.referenceOfId(REF_1);
        final Reference<Channel> reference2 = Channel.referenceOfId(REF_2);

        existingSupplyChannels = asList(channel1, channel2);
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
                InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, expandedReference1),
                InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_2)),
                InventoryEntryDraft.of(SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2,null),
                InventoryEntryDraft.of(SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2, expandedReference1),
                InventoryEntryDraft.of(SKU_2, QUANTITY_2, DATE_2, RESTOCKABLE_2, Channel.referenceOfId(KEY_2)),
                InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,null),
                InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1, expandedReference1),
                InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_2))
        );
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
        assertThat(stats.getUpdated()).isEqualTo(3);
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
        assertThat(stats.getUpdated()).isEqualTo(3);
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
    }

    @Test
    public void sync_WithNotEnsuredChannels_ShouldNotSyncEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithNewChannel))
                .toCompletableFuture()
                .join();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
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
    }

    @Test
    public void sync_WithExceptionWhenFetchingAllChannels_ShouldNotProcessAnything() {
        final InventorySyncOptions options = getInventorySyncOptions(30, false);
        final InventoryService service = getMockInventoryService(existingSupplyChannels, existingInventories,
                getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.fetchAllSupplyChannels()).thenReturn(getCompletionStageWithException());
        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();
        assertThat(stats.getProcessed()).isEqualTo(0);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
    }

    @Test
    public void sync_WithExceptionWhenFetchingExistingInventoriesBatch_ShouldNotProcessThatBatch() {
        final InventorySyncOptions options = getInventorySyncOptions(1, false);
        final InventoryService service = getMockInventoryService(existingSupplyChannels, existingInventories,
                getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.fetchInventoryEntriesBySkus(singleton(SKU_1))).thenReturn(getCompletionStageWithException());
        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(6);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(3);
        assertThat(stats.getUpdated()).isEqualTo(3);
    }

    @Test
    public void sync_WithExceptionWhenCreatingOrUpdatingEntries_ShouldNotSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false);
        final InventoryService service = getMockInventoryService(existingSupplyChannels, existingInventories,
                getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.createInventoryEntry(any())).thenReturn(getCompletionStageWithException());
        when(service.updateInventoryEntry(any(), any())).thenReturn(getCompletionStageWithException());
        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(9);
        assertThat(stats.getFailed()).isEqualTo(6);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
    }

    @Test
    public void sync_WithExceptionWhenCreatingNewSupplyChannel_ShouldTriggerErrorCallbackAndIncrementFailed() {
        final List<String> callbackMessages = new ArrayList<>();
        final List<Throwable> callbackThrowables = new ArrayList<>();
        final BiConsumer<String, Throwable> collectingErrorCallback = (msg, throwable) -> {
            callbackMessages.add(msg);
            if (throwable != null) {
                callbackThrowables.add(throwable);
            }
        };

        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(mock(SphereClient.class))
            .ensureChannels(true)
            .setErrorCallBack(collectingErrorCallback)
            .build();
        final InventoryService service = getMockInventoryService(existingSupplyChannels, existingInventories,
            getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.createSupplyChannel(any())).thenReturn(getCompletionStageWithException());
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraft
            .of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        assertThat(callbackMessages.get(0)).isEqualTo(format("Failed to create new supply channel of key '%s'.",
            KEY_3));
        assertThat(callbackThrowables.get(0)).isExactlyInstanceOf(CompletionException.class);
    }

    @Test
    public void sync_WithNullInInputList_ShouldIncrementFailedStatistics() {
        final InventoryService service = getMockInventoryService(existingSupplyChannels, existingInventories,
            getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        final InventorySyncOptions options = getInventorySyncOptions(3, false);
        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(null))
            .toCompletableFuture()
            .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
    }

    @Test
    public void sync_WithChannelNotPresentInMap_ShouldIncrementFailedStatistics() {
        final InventorySyncOptions options = getInventorySyncOptions(30, false);
        final Channel oldChannel = getMockSupplyChannel(REF_1, KEY_1);
        final Reference<Channel> oldChannelReference = Channel.referenceOfId(REF_1).filled(oldChannel);
        final List<InventoryEntry> oldInventories = singletonList(
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, oldChannelReference, null));
        final InventoryService service = getMockInventoryService(emptyList(), oldInventories,
            getMockSupplyChannel(REF_2, KEY_2), mock(InventoryEntry.class), mock(InventoryEntry.class));
        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraft
            .of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_1));
        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
    }

    @Test
    public void sync_With409ErrorDuringUpdate_ShouldRetryUpdate() {
        final InventoryEntry oldInventory =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null);
        final InventoryEntry oldInventoryUpdated =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_2, DATE_1, null, null);
        final InventoryEntryDraft newEntry = InventoryEntryDraft.of(SKU_1, QUANTITY_2, DATE_1, RESTOCKABLE_1,null);

        final InventorySyncOptions options = getInventorySyncOptions(1, false);
        final InventoryService service = getMockInventoryService(emptyList(), singletonList(oldInventory),
            getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.fetchInventoryEntry(SKU_1, null))
            .thenReturn(completedFuture(Optional.of(oldInventoryUpdated)));
        when(service.updateInventoryEntry(eq(oldInventory), any()))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()));
        when(service.updateInventoryEntry(eq(oldInventoryUpdated), any()))
            .thenReturn(completedFuture(mock(InventoryEntry.class)));

        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newEntry)).toCompletableFuture()
            .join();

        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(1);
        verify(service, times(1)).fetchInventoryEntry(eq(SKU_1), any());
        verify(service, times(2)).updateInventoryEntry(any(), any());
        verify(service, times(1)).updateInventoryEntry(eq(oldInventory), any());
        verify(service, times(1)).updateInventoryEntry(eq(oldInventoryUpdated), any());
    }

    @Test
    public void sync_With409ErrorDuringUpdateAndThenOtherErrorDuringRetry_ShouldFail() {
        final InventoryEntry oldInventory =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null);
        final InventoryEntry oldInventoryUpdated =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_2, DATE_1, null, null);
        final InventoryEntryDraft newEntry = InventoryEntryDraft.of(SKU_1, QUANTITY_2, DATE_1, RESTOCKABLE_1,null);

        final InventorySyncOptions options = getInventorySyncOptions(1, false);
        final InventoryService service = getMockInventoryService(emptyList(), singletonList(oldInventory),
            getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.fetchInventoryEntry(SKU_1, null))
            .thenReturn(completedFuture(Optional.of(oldInventoryUpdated)));
        when(service.updateInventoryEntry(eq(oldInventory), any()))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()));
        when(service.updateInventoryEntry(eq(oldInventoryUpdated), any()))
            .thenReturn(exceptionallyCompletedFuture(new NotFoundException()));

        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newEntry)).toCompletableFuture()
            .join();

        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        verify(service, times(1)).fetchInventoryEntry(eq(SKU_1), any());
        verify(service, times(2)).updateInventoryEntry(any(), any());
        verify(service, times(1)).updateInventoryEntry(eq(oldInventory), any());
        verify(service, times(1)).updateInventoryEntry(eq(oldInventoryUpdated), any());
    }

    @Test
    public void sync_With409ErrorDuringUpdateAndThenNotFoundLatestVersion_ShouldFail() {
        final InventoryEntry oldInventory =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null);
        final InventoryEntryDraft newEntry = InventoryEntryDraft.of(SKU_1, QUANTITY_2, DATE_1, RESTOCKABLE_1,null);

        final InventorySyncOptions options = getInventorySyncOptions(1, false);
        final InventoryService service = getMockInventoryService(emptyList(), singletonList(oldInventory),
            getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.fetchInventoryEntry(SKU_1, null))
            .thenReturn(completedFuture(Optional.empty()));
        when(service.updateInventoryEntry(eq(oldInventory), any()))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()));

        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newEntry)).toCompletableFuture()
            .join();

        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        verify(service, times(1)).fetchInventoryEntry(eq(SKU_1), any());
        verify(service, times(1)).updateInventoryEntry(any(), any());
        verify(service, times(1)).updateInventoryEntry(eq(oldInventory), any());
    }

    @Test
    public void sync_WithConstant409ErrorDuringUpdate_ShouldEventuallyFail() {
        final InventoryEntry oldInventory =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1, null, null);
        final InventoryEntry oldInventoryUpdated =
            getMockInventoryEntry(SKU_1, QUANTITY_1, RESTOCKABLE_2, DATE_1, null, null);
        final InventoryEntryDraft newEntry = InventoryEntryDraft.of(SKU_1, QUANTITY_2, DATE_1, RESTOCKABLE_1,null);

        final InventorySyncOptions options = getInventorySyncOptions(1, false);
        final InventoryService service = getMockInventoryService(emptyList(), singletonList(oldInventory),
            getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(service.fetchInventoryEntry(SKU_1, null))
            .thenReturn(completedFuture(Optional.of(oldInventoryUpdated)));
        when(service.updateInventoryEntry(any(), any()))
            .thenReturn(exceptionallyCompletedFuture(new ConcurrentModificationException()));

        final InventorySync inventorySync = new InventorySync(options, service, mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newEntry)).toCompletableFuture()
            .join();

        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(0);
        assertThat(stats.getUpdated()).isEqualTo(0);
        verify(service, times(ATTEMPTS_ON_409_LIMIT)).fetchInventoryEntry(eq(SKU_1), any());
        verify(service, times(ATTEMPTS_ON_409_LIMIT + 1)).updateInventoryEntry(any(), any());
    }

    private InventorySync getInventorySync(int batchSize, boolean ensureChannels) {
        final InventorySyncOptions options = getInventorySyncOptions(batchSize, ensureChannels);
        final InventoryService service = getMockInventoryService(existingSupplyChannels, existingInventories,
                getMockSupplyChannel(REF_3, KEY_3), mock(InventoryEntry.class), mock(InventoryEntry.class));
        return new InventorySync(options, service, mock(TypeService.class));
    }

    private InventorySyncOptions getInventorySyncOptions(int batchSize, boolean ensureChannels) {
        return InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                .setBatchSize(batchSize)
                .ensureChannels(ensureChannels)
                .build();
    }
}
