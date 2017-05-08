package com.commercetools.sync.inventory;

import com.commercetools.sync.commons.helpers.CtpClient;
import com.commercetools.sync.inventory.helpers.InventorySyncStatistics;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySyncTest.class);

    private final static String SKU_1 = "1000";
    private final static String SKU_2 = "2000";
    private final static String SKU_3 = "3000";

    private final static String KEY_1 = "channel-key_1";
    private final static String KEY_2 = "channel-key_2";
    private final static String KEY_3 = "channel-key_3";

    private final static String REF_1 = "111";
    private final static String REF_2 = "222";
    private final static String REF_3 = "333";

    private static final String CTP_KEY = "testKey";
    private static final String CTP_ID = "testId";
    private static final String CTP_SECRET = "testSecret";

    private final Long QUANTITY_1 = 10l;
    private final Long QUANTITY_2 = 30l;

    private final Integer RESTOCKABLE_1 = 10;
    private final Integer RESTOCKABLE_2 = 10;

    private final ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private final ZonedDateTime DATE_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private List<InventoryEntryDraft> drafts;
    private List<InventoryEntry> inventoriesDB;
    private List<Channel> channelsDB;

    {
        Channel channel1 = mockChannel(REF_1, KEY_1);
        Channel channel2 = mockChannel(REF_2, KEY_2);
        Channel channel3 = mockChannel(REF_3, KEY_3);

        Reference<Channel> reference1 = Channel.referenceOfId(REF_1).filled(channel1);

        channelsDB = asList(channel1, channel2);
        inventoriesDB = asList(
                InventoryEntryMock.of(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1).build(),
                InventoryEntryMock.of(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1).withChannelRefExpanded(REF_1, KEY_1).build(),
                InventoryEntryMock.of(SKU_1, QUANTITY_1, RESTOCKABLE_1, DATE_1).withChannelRefExpanded(REF_2, KEY_2).build(),
                InventoryEntryMock.of(SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1).build(),
                InventoryEntryMock.of(SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1).withChannelRefExpanded(REF_1, KEY_1).build(),
                InventoryEntryMock.of(SKU_2, QUANTITY_1, RESTOCKABLE_1, DATE_1).withChannelRefExpanded(REF_2, KEY_2).build()
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
    public void syncDrafts_ShouldEndWithoutExceptions() {
        getInventorySyncer(30, 1, false).syncDrafts(drafts);
    }

    @Test
    public void syncDrafts_WithParallelOption_ShouldEndWithoutExceptions() {
        final List<InventoryEntryDraft> moreDrafts = Stream.generate(() -> drafts)
                .limit(20)
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());
        getInventorySyncer(10, 4, false).syncDrafts(moreDrafts);
    }

    @Test
    public void getStatistics_ShouldReturnProperValues() {
        final InventorySync inventorySync = getInventorySyncer(30, 1, false);
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
        final InventorySync inventorySync = getInventorySyncer(30, 1, true);
        inventorySync.syncDrafts(asList(draftWithNewChannel));
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getCreated()).isEqualTo(1);
    }

    @Test
    public void syncDrafts_WithNotEnsuredChannels_ShouldNotSyncEntriesWithUnknownChannels() {
        final InventoryEntryDraft draftWithNewChannel = InventoryEntryDraft.of(SKU_3, QUANTITY_1, DATE_1, RESTOCKABLE_1,
                Channel.referenceOfId(KEY_3));
        final InventorySync inventorySync = getInventorySyncer(30, 1, false);
        inventorySync.syncDrafts(asList(draftWithNewChannel));
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(1);
        assertThat(stats.getFailed()).isEqualTo(1);
    }

    @Test
    public void syncDrafts_WithDraftsWithNullSku_ShouldNotSync() {
        final InventoryEntryDraft draftWithNullSku = InventoryEntryDraft.of(null, 12);
        final InventorySync inventorySync = getInventorySyncer(30, 1, false);
        inventorySync.syncDrafts(asList(draftWithNullSku));
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
        final InventorySync inventorySync = new InventorySync(InventorySyncOptionsBuilder.of(mockCtpClient())
                .build(), mockThrowingInventoryService(), mock(TypeService.class));

        inventorySync.syncDrafts(toProcess);
        final InventorySyncStatistics stats = inventorySync.getStatistics();
        assertThat(stats.getProcessed()).isEqualTo(10);
        assertThat(stats.getFailed()).isEqualTo(7);
    }

    private InventorySync getInventorySyncer(int batchSize, int parallelThreads, boolean ensureChannels) {
        final InventorySyncOptions options = InventorySyncOptionsBuilder.of(mockCtpClient())
                .setBatchSize(batchSize)
                .setParallelProcessing(parallelThreads)
                .ensureChannels(ensureChannels)
                .build();
        return new InventorySync(options, mockInventoryService(), mock(TypeService.class));
    }

    private CtpClient mockCtpClient() {
        final CtpClient ctpClient = mock(CtpClient.class);
        final BlockingSphereClient client = mock(BlockingSphereClient.class);
        when(ctpClient.getClientConfig()).thenReturn(SphereClientConfig.of(CTP_KEY, CTP_ID, CTP_SECRET));
        when(ctpClient.getClient()).thenReturn(client);
        return ctpClient;
    }

    private InventoryService mockInventoryService() {
        final Channel mockedChannel = mockChannel(KEY_3, REF_3);
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.fetchAllSupplyChannels()).thenReturn(channelsDB);
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(inventoriesDB);
        when(inventoryService.createSupplyChannel(any())).thenReturn(mockedChannel);
        when(inventoryService.createInventoryEntry(any())).thenReturn(null);
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(null);
        return inventoryService;
    }

    /**
     *
     * @return mock of {@link InventoryService} that throws {@link RuntimeException} on creating and updating calls
     */
    private InventoryService mockThrowingInventoryService() {
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.fetchAllSupplyChannels()).thenReturn(channelsDB);
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(inventoriesDB);
        when(inventoryService.createSupplyChannel(any())).thenThrow(new RuntimeException());
        when(inventoryService.createInventoryEntry(any())).thenThrow(new RuntimeException());
        when(inventoryService.updateInventoryEntry(any(), any())).thenThrow(new RuntimeException());
        return inventoryService;
    }

    private Channel mockChannel(String id, String key) {
        Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(id);
        when(channel.getKey()).thenReturn(key);
        return channel;
    }
}
