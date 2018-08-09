package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.commercetools.sync.inventories.utils.InventoryReferenceReplacementUtils.replaceChannelReferenceIdWithKey;
import static com.commercetools.sync.inventories.utils.InventoryReferenceReplacementUtils.replaceInventoriesReferenceIdsWithKeys;
import static com.commercetools.sync.products.ProductSyncMockUtils.getChannelMock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventoryReferenceReplacementUtilsTest {

    @Test
    public void
        replaceInventoriesReferenceIdsWithKeys_WithAllExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        //preparation
        final String customTypeId = UUID.randomUUID().toString();
        final String customTypeKey = "customTypeKey";
        final Type mockCustomType = mock(Type.class);
        when(mockCustomType.getId()).thenReturn(customTypeId);
        when(mockCustomType.getKey()).thenReturn(customTypeKey);

        final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
        final String channelKey = "channelKey";

        for (int i = 0; i < 10; i++) {
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
            replaceInventoriesReferenceIdsWithKeys(mockInventoryEntries);

        //assertion
        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeKey);
            assertThat(referenceReplacedDraft.getSupplyChannel().getId()).isEqualTo(channelKey);
        }
    }

    @Test
    public void
        replaceInventoriesReferenceIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        //preparation
        final String customTypeId = UUID.randomUUID().toString();
        final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
        final String channelId = UUID.randomUUID().toString();

        for (int i = 0; i < 10; i++) {
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
            replaceInventoriesReferenceIdsWithKeys(mockInventoryEntries);

        //assertion
        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
            assertThat(referenceReplacedDraft.getSupplyChannel().getId()).isEqualTo(channelId);
        }
    }


    @Test
    public void replaceChannelReferenceIdWithKey_WithNonExpandedReferences_ShouldReturnReferenceWithoutReplacedKeys() {
        //preparation
        final String channelId = UUID.randomUUID().toString();
        final Reference<Channel> channelReference = Channel.referenceOfId(channelId);
        final InventoryEntry inventoryEntry = mock(InventoryEntry.class);
        when(inventoryEntry.getSupplyChannel()).thenReturn(channelReference);

        //test
        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(inventoryEntry);

        //assertion
        assertThat(channelReferenceWithKey).isNotNull();
        assertThat(channelReferenceWithKey.getId()).isEqualTo(channelId);
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithExpandedReferences_ShouldReturnReplaceReferenceIdsWithKey() {
        //preparation
        final String channelKey = "channelKey";
        final Channel channel = getChannelMock(channelKey);
        final Reference<Channel> channelReference = Reference
            .ofResourceTypeIdAndIdAndObj(Channel.referenceTypeId(), channel.getId(), channel);
        final InventoryEntry inventoryEntry = mock(InventoryEntry.class);
        when(inventoryEntry.getSupplyChannel()).thenReturn(channelReference);

        //test
        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(inventoryEntry);

        //assertion
        assertThat(channelReferenceWithKey).isNotNull();
        assertThat(channelReferenceWithKey.getId()).isEqualTo(channelKey);
    }

    @Test
    public void replaceChannelReferenceIdWithKey_WithNullChannelReference_ShouldReturnNull() {
        final InventoryEntry inventoryEntry = mock(InventoryEntry.class);

        final Reference<Channel> channelReferenceWithKey = replaceChannelReferenceIdWithKey(inventoryEntry);

        assertThat(channelReferenceWithKey).isNull();
    }
}
