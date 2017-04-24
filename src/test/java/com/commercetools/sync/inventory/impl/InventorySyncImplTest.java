package com.commercetools.sync.inventory.impl;

import com.commercetools.sync.commons.utils.ClientConfigurationUtils;
import com.commercetools.sync.inventory.InventoryEntryMock;
import com.commercetools.sync.inventory.InventorySync;
import com.commercetools.sync.inventory.helpers.InventorySyncOptions;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.client.SphereClientConfig;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySyncImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventorySyncImplTest.class);

    private final static String SKU_1 = "1000";
    private final static String SKU_2 = "2000";
    private final static String SKU_3 = "3000";

    private final static String KEY_1 = "channel-key_1";
    private final static String KEY_2 = "channel-key_2";
    private final static String KEY_3 = "channel-key_3";

    private final static String REF_1 = "111";
    private final static String REF_2 = "222";
    private final static String REF_3 = "333";

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

        channelsDB = asList(channel1, channel2, channel3);
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
    public void syncInventoryDrafts_returnsWithoutExceptions() {
        getInventorySyncer().syncInventoryDrafts(drafts);
    }

    @Test
    public void syncInventoryDrafts_returnsWithoutExceptions_havingParallelOption() {
        final InventorySyncOptions options = mock(InventorySyncOptions.class);
        when(options.getParallelProcessing()).thenReturn(4);
        when(options.getClientConfig()).thenReturn(SphereClientConfig.of("123","123","123"));
        final List<InventoryEntryDraft> moreDrafts = Stream.generate(() -> drafts)
                .limit(20)
                .flatMap(list -> list.stream())
                .collect(Collectors.toList());
        (new InventorySyncImpl(options, mockInventoryService())).syncInventoryDrafts(moreDrafts);
    }

    private InventorySync getInventorySyncer() {
        final InventorySyncOptions options = mock(InventorySyncOptions.class);
        when(options.getClientConfig()).thenReturn(SphereClientConfig.of("123","123","123"));
        return new InventorySyncImpl(options, mockInventoryService());
    }

    private InventoryService mockInventoryService() {
        final InventoryService inventoryService = mock(InventoryService.class);
        when(inventoryService.fetchAllSupplyChannels()).thenReturn(channelsDB);
        when(inventoryService.fetchInventoryEntriesBySkus(any())).thenReturn(inventoriesDB);
        when(inventoryService.createInventoryEntry(any())).thenReturn(null);
        when(inventoryService.updateInventoryEntry(any(), any())).thenReturn(null);
        return inventoryService;
    }

    private TypeService mockTypeService() {
        final TypeService typeService = mock(TypeService.class);
        return typeService;
    }

    private Channel mockChannel(String id, String key) {
        Channel channel = mock(Channel.class);
        when(channel.getId()).thenReturn(id);
        when(channel.getKey()).thenReturn(key);
        return channel;
    }
}
