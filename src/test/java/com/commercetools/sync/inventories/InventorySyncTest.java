package com.commercetools.sync.inventories;

import com.commercetools.sync.inventories.helpers.InventorySyncStatistics;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.commercetools.sync.commons.MockUtils.getMockCtpClient;
import static com.commercetools.sync.inventories.InventorySyncMockUtils.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InventorySyncTest {

    private final static String SKU_1 = "1000";
    private final static String SKU_2 = "2000";
    private final static String SKU_3 = "3000";

    private final static String KEY_1 = "channel-key_1";
    private final static String KEY_2 = "channel-key_2";
    private final static String KEY_3 = "channel-key_3";

    private final static String REF_1 = "111";
    private final static String REF_2 = "222";
    private final static String REF_3 = "333";

    private final static Long QUANTITY_1 = 10l;
    private final static Long QUANTITY_2 = 30l;

    private final static Integer RESTOCKABLE_1 = 10;
    private final static Integer RESTOCKABLE_2 = 10;

    private final static ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private final static ZonedDateTime DATE_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private List<InventoryEntryDraft> drafts;
    private List<InventoryEntry> inventoriesDB;
    private List<Channel> channelsDB;

    @Before
    public void setup() {
        final Channel channel1 = getMockSupplyChannel(REF_1, KEY_1);
        final Channel channel2 = getMockSupplyChannel(REF_2, KEY_2);

        final Reference<Channel> reference1 = Channel.referenceOfId(REF_1).filled(channel1);
        final Reference<Channel> reference2 = Channel.referenceOfId(REF_2).filled(channel2);

        channelsDB = asList(channel1, channel2);
        inventoriesDB = asList(
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
    }

    @Test
    public void getStatistics_ShouldReturnProperValues() {
        final InventorySync inventorySync = getInventorySyncer(30, false);
        inventorySync.syncDrafts(drafts);
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats).isNotNull();
        assertThat(stats.getProcessed()).isEqualTo(9);
        assertThat(stats.getFailed()).isEqualTo(0);
        assertThat(stats.getCreated()).isEqualTo(3);
        assertThat(stats.getUpdated()).isEqualTo(3);
    }

    @Test
    public void syncDrafts_WithEnsuredChannels_ShouldCreateEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = getInventorySyncer(30, true);
        inventorySync.syncDrafts(singletonList(draftWithNewChannel));
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(1);
    }

    @Test
    public void syncDrafts_WithNotEnsuredChannels_ShouldNotSyncEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = getInventorySyncer(30, false);
        inventorySync.syncDrafts(singletonList(draftWithNewChannel));
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
    }

    @Test
    public void syncDrafts_WithDraftsWithNullSku_ShouldNotSync() {
        final InventoryEntryDraft draftWithNullSku = InventoryEntryDraft.of(null, 12);
        final InventorySync inventorySync = getInventorySyncer(30, false);
        inventorySync.syncDrafts(singletonList(draftWithNullSku));
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(0);
        assertThat(stats.getUnprocessedDueToEmptySku()).isEqualTo(1);
    }

    @Test
    public void syncDrafts_WithProblemsEncountered_ShouldEndWithoutException() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));
        final List<InventoryEntryDraft> toProcess = new ArrayList<>(drafts);
        toProcess.add(draftWithNewChannel);
        final InventoryService service = getMockThrowingInventoryService(channelsDB, inventoriesDB);
        final InventorySync inventorySync = new InventorySync(InventorySyncOptionsBuilder.of(getMockCtpClient())
                .build(), service, mock(TypeService.class));

        inventorySync.syncDrafts(toProcess);
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(10);
        assertThat(stats.getFailed()).isEqualTo(7);
    }

    private InventorySync getInventorySyncer(int batchSize, boolean ensureChannels) {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(getMockCtpClient())
                .setBatchSize(batchSize)
                .ensureChannels(ensureChannels)
                .build();
        final InventoryService service = getMockInventoryService(channelsDB, inventoriesDB,
                getMockSupplyChannel(REF_3, KEY_3), null, null);
        return new InventorySync(options, service, mock(TypeService.class));
    }
}
