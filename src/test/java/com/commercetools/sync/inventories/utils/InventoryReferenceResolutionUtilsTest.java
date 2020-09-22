package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.expansion.ExpansionPath;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.queries.InventoryEntryQuery;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.buildInventoryQuery;
import static com.commercetools.sync.inventories.utils.InventoryReferenceResolutionUtils.mapToInventoryEntryDrafts;
import static com.commercetools.sync.products.ProductSyncMockUtils.getChannelMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryReferenceResolutionUtilsTest {

    @Test
    void mapToInventoryEntryDrafts_WithAllExpandedReferences_ShouldReturnResourceIdentifiersWithKeys() {
        //preparation
        final String customTypeId = UUID.randomUUID().toString();
        final String customTypeKey = "customTypeKey";
        final Type mockCustomType = mock(Type.class);
        when(mockCustomType.getId()).thenReturn(customTypeId);
        when(mockCustomType.getKey()).thenReturn(customTypeKey);

        final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
        final String channelKey = "channelKey";

        for (int i = 0; i < 2; i++) {
            final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndObj(UUID.randomUUID().toString(),
                mockCustomType);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);

            final Channel channel = getChannelMock(channelKey);
            final Reference<Channel> channelReference = Reference
                .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);
            when(mockInventoryEntry.getSupplyChannel()).thenReturn(channelReference);

            mockInventoryEntries.add(mockInventoryEntry);
        }

        //test
        final List<InventoryEntryDraft> referenceReplacedDrafts =
            mapToInventoryEntryDrafts(mockInventoryEntries);

        //assertion
        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom().getType().getKey()).isEqualTo(customTypeKey);
            assertThat(referenceReplacedDraft.getSupplyChannel().getKey()).isEqualTo(channelKey);
        }
    }

    @Test
    void mapToInventoryEntryDrafts_WithNonExpandedReferences_ShouldReturnResourceIdentifiersWithoutKeys() {
        //preparation
        final String customTypeId = UUID.randomUUID().toString();
        final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
        final String channelId = UUID.randomUUID().toString();

        for (int i = 0; i < 2; i++) {
            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Type.referenceOfId(customTypeId);
            when(mockCustomFields.getType()).thenReturn(typeReference);

            final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
            when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);

            final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
            when(mockInventoryEntry.getSupplyChannel()).thenReturn(channelReference);

            mockInventoryEntries.add(mockInventoryEntry);
        }

        //test
        final List<InventoryEntryDraft> referenceReplacedDrafts =
            mapToInventoryEntryDrafts(mockInventoryEntries);

        //assertion
        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
            assertThat(referenceReplacedDraft.getSupplyChannel().getId()).isEqualTo(channelId);
        }
    }

    @Test
    void mapToInventoryEntryDrafts_WithNullReferences_ShouldNotReturnResourceIdentifiers() {
        //test
        final List<InventoryEntryDraft> referenceReplacedDrafts =
            mapToInventoryEntryDrafts(Collections.singletonList(mock(InventoryEntry.class)));

        //assertion
        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom()).isNull();
            assertThat(referenceReplacedDraft.getSupplyChannel()).isNull();
        }
    }

    @Test
    void buildInventoryQuery_Always_ShouldReturnQueryWithAllNeededReferencesExpanded() {
        final InventoryEntryQuery inventoryEntryQuery = buildInventoryQuery();
        assertThat(inventoryEntryQuery.expansionPaths()).containsExactly(
            ExpansionPath.of("custom.type"),
            ExpansionPath.of("supplyChannel"));
    }

}
