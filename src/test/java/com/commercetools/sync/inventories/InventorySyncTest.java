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
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.SphereException;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.commercetools.sync.commons.asserts.statistics.AssertionsForStatistics.assertThat;
import static com.commercetools.sync.commons.helpers.BaseReferenceResolver.BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getCompletionStageWithException;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockChannelService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryEntry;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockInventoryService;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.getMockSupplyChannel;
import static io.sphere.sdk.utils.CompletableFutureUtils.failed;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        final Reference<Channel> expandedReference1 = Channel.referenceOfId(REF_1).filled(channel1);

        final Reference<Channel> reference1 = Channel.referenceOfId(REF_1);
        final Reference<Channel> reference2 = Channel.referenceOfId(REF_2);

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

        assertThat(stats).hasValues(9, 3, 3, 0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithEmptyList_ShouldNotSync() {
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(emptyList())
                .toCompletableFuture()
                .join();

        assertThat(stats).hasValues(0, 0, 0, 0);
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

        assertThat(stats).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackMessages).hasSize(0);
        assertThat(errorCallBackExceptions).hasSize(0);
    }

    @Test
    public void sync_WithNotEnsuredChannels_ShouldNotSyncEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));

        final InventorySyncOptions options = getInventorySyncOptions(30, false);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(completedFuture(Optional.empty()));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithNewChannel))
                .toCompletableFuture()
                .join();

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(format("Failed to resolve references on InventoryEntryDraft"
                + " with SKU:'%s'. Reason: %s: Failed to resolve supply channel reference on InventoryEntryDraft with"
                + " SKU:'%s'. Reason: Channel with key '%s' does not exist.", SKU_3,
            ReferenceResolutionException.class.getCanonicalName(), SKU_3, KEY_3));
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithDraftsWithNullSku_ShouldNotSync() {
        final InventoryEntryDraft draftWithNullSku = InventoryEntryDraft.of(null, 12);
        final InventorySync inventorySync = getInventorySync(30, false);
        final InventorySyncStatistics stats = inventorySync.sync(singletonList(draftWithNullSku))
                .toCompletableFuture()
                .join();

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to process inventory entry without SKU.");
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

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to process inventory entry without SKU.");
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    @Test
    public void sync_WithExceptionWhenFetchingExistingInventoriesBatch_ShouldProcessThatBatch() {
        final InventorySyncOptions options = getInventorySyncOptions(1, false);

        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3));
        when(inventoryService.fetchInventoryEntriesBySkus(singleton(SKU_1)))
            .thenReturn(getCompletionStageWithException());

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();

        assertThat(stats).hasValues(9, 5, 1, 3);
        assertThat(errorCallBackMessages).hasSize(3);
        assertThat(errorCallBackExceptions).hasSize(3);
    }

    @Test
    public void sync_WithExceptionWhenCreatingOrUpdatingEntries_ShouldNotSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(inventoryService.createInventoryEntry(any())).thenReturn(getCompletionStageWithException());
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(getCompletionStageWithException());

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_2, KEY_2));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));
        final InventorySyncStatistics stats = inventorySync.sync(drafts)
                .toCompletableFuture()
                .join();

        assertThat(stats).hasValues(9, 0, 0, 6);
        assertThat(errorCallBackMessages).hasSize(6);
        assertThat(errorCallBackExceptions).hasSize(6);
    }

    @Test
    public void sync_WithExceptionWhenUpdatingEntries_ShouldNotSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(getCompletionStageWithException());

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_1, KEY_1));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraftBuilder
            .of(SKU_1, QUANTITY_2, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(REF_1)).build();

        final InventorySyncStatistics stats = inventorySync.sync(Collections.singletonList(inventoryEntryDraft))
                                                           .toCompletableFuture()
                                                           .join();

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(
            format("Failed to update inventory entry of SKU '%s' and supply channel id '%s'.", SKU_1, REF_1));
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void sync_WithExceptionWhenCreatingEntries_ShouldNotSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        when(inventoryService.createInventoryEntry(any())).thenReturn(getCompletionStageWithException());

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_1, KEY_1));

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventoryEntryDraft inventoryEntryDraft = InventoryEntryDraftBuilder
            .of(SKU_3, QUANTITY_2, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(REF_1)).build();

        final InventorySyncStatistics stats = inventorySync.sync(Collections.singletonList(inventoryEntryDraft))
                                                           .toCompletableFuture()
                                                           .join();

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).hasSize(1);
        assertThat(errorCallBackExceptions).hasSize(1);
        assertThat(errorCallBackMessages.get(0)).isEqualTo(
            format("Failed to create inventory entry of SKU '%s' and supply channel id '%s'.", SKU_3, REF_1));
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void sync_WithExistingInventoryEntryButWithEmptyCustomTypeReference_ShouldFailSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, false);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = mock(ChannelService.class);
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(completedFuture(Optional.of(REF_2)));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final List<InventoryEntryDraft> newDrafts = new ArrayList<>();
        final InventoryEntryDraft draftWithNullCustomTypeId =
            InventoryEntryDraft.of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, null)
                               .withCustom(CustomFieldsDraft.ofTypeIdAndJson("", new HashMap<>()));
        newDrafts.add(draftWithNullCustomTypeId);

        final InventorySyncStatistics syncStatistics = inventorySync.sync(newDrafts).toCompletableFuture().join();

        assertThat(syncStatistics).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).contains(format("Failed to resolve references on"
            + " InventoryEntryDraft with SKU:'%s'. Reason: %s: Failed to resolve custom type reference on "
            + "InventoryEntryDraft with SKU:'1000'. Reason: %s", SKU_1,
            ReferenceResolutionException.class.getCanonicalName(), BLANK_ID_VALUE_ON_RESOURCE_IDENTIFIER));
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
    }

    @Test
    public void sync_WithNewSupplyChannelAndEnsure_ShouldSync() {
        final InventorySyncOptions options = getInventorySyncOptions(3, true);

        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3));
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraft
            .of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newInventoryDraft))
                                                           .toCompletableFuture()
                                                           .join();
        assertThat(stats).hasValues(1, 1, 0, 0);
        assertThat(errorCallBackMessages).isEmpty();
        assertThat(errorCallBackExceptions).isEmpty();
    }

    @Test
    public void sync_WithExceptionWhenCreatingNewSupplyChannel_ShouldTriggerErrorCallbackAndIncrementFailed() {
        final InventorySyncOptions options = getInventorySyncOptions(3, true);

        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3));
        when(channelService.fetchCachedChannelId(anyString()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(channelService.createAndCacheChannel(anyString())).thenReturn(failed(new SphereException()));

        final InventoryEntryDraft newInventoryDraft = InventoryEntryDraft
            .of(SKU_1, QUANTITY_1, DATE_1, RESTOCKABLE_1, Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(newInventoryDraft))
            .toCompletableFuture()
            .join();

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).contains(format("Failed to resolve supply channel reference on"
                + " InventoryEntryDraft with SKU:'%s'.", SKU_1));
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()).isExactlyInstanceOf(ReferenceResolutionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause().getCause()).isExactlyInstanceOf(CompletionException.class);
        assertThat(errorCallBackExceptions.get(0).getCause()
                                          .getCause().getCause()).isExactlyInstanceOf(SphereException.class);
    }

    @Test
    public void sync_WithNullInInputList_ShouldIncrementFailedStatistics() {
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));

        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_3, KEY_3));
        final InventorySyncOptions options = getInventorySyncOptions(3, false);

        final InventorySync inventorySync = new InventorySync(options, inventoryService, channelService,
            mock(TypeService.class));

        final InventorySyncStatistics stats = inventorySync.sync(singletonList(null))
            .toCompletableFuture()
            .join();

        assertThat(stats).hasValues(1, 0, 0, 1);
        assertThat(errorCallBackMessages).isNotEmpty();
        assertThat(errorCallBackMessages.get(0)).isEqualTo("Failed to process null inventory draft.");
        assertThat(errorCallBackExceptions).isNotEmpty();
        assertThat(errorCallBackExceptions.get(0)).isEqualTo(null);
    }

    private InventorySync getInventorySync(int batchSize, boolean ensureChannels) {
        final InventorySyncOptions options = getInventorySyncOptions(batchSize, ensureChannels);
        final InventoryService inventoryService = getMockInventoryService(existingInventories,
            mock(InventoryEntry.class), mock(InventoryEntry.class));
        final ChannelService channelService = getMockChannelService(getMockSupplyChannel(REF_2, KEY_2));
        return new InventorySync(options, inventoryService, channelService, mock(TypeService.class));
    }

    private InventorySyncOptions getInventorySyncOptions(int batchSize, boolean ensureChannels) {
        return InventorySyncOptionsBuilder.of(mock(SphereClient.class))
                                          .batchSize(batchSize)
                                          .ensureChannels(ensureChannels)
                                          .errorCallback((callBackError, exception) -> {
                                              errorCallBackMessages.add(callBackError);
                                              errorCallBackExceptions.add(exception);
                                          })
                                          .build();
    }
}
