package com.commercetools.sync.inventory.utils;

import com.commercetools.sync.inventory.InventoryEntryMock;
import com.commercetools.sync.inventory.helpers.InventorySyncOptions;
import com.commercetools.sync.services.TypeService;
import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InventorySyncUtilsTest {

    private static final ZonedDateTime DATE_1 = ZonedDateTime.of(2017, 4, 1, 10, 0, 0, 0, ZoneId.of("UTC"));
    private static final ZonedDateTime DATE_2 = ZonedDateTime.of(2017, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC"));

    private final InventoryEntry inventoryEntry;
    private final InventoryEntryDraft similarDraft;
    private final InventoryEntryDraft variousDraft;

    {
        inventoryEntry = InventoryEntryMock.of("123", 10l, 10, DATE_1).withChannelRefExpanded("111", "key1").build();
        similarDraft = InventoryEntryDraftBuilder.of("123", 10l, DATE_1, 10, Channel.referenceOfId("111")).build();
        variousDraft = InventoryEntryDraftBuilder.of("321", 20l, DATE_2, 20, Channel.referenceOfId("222")).build();
    }

    @Test
    public void buidActions_returnsEmptyList_havingSimilarEntries() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntry, similarDraft, mock(InventorySyncOptions.class));

        assertThat(actions).isEmpty();
    }

    @Test
    public void buildActions_returnsActions_havingVariouEntries() {
        List<UpdateAction<InventoryEntry>> actions = InventorySyncUtils
                .buildActions(inventoryEntry, variousDraft, mock(InventorySyncOptions.class));

        assertThat(actions).hasSize(4);
    }
}
