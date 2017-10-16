package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.Type;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventoryReferenceReplacementUtilsTest {

    @Test
    public void
    replaceInventoriesReferenceIdsWithKeys_WithAllExpandedReferences_ShouldReturnReferencesWithReplacedKeys() {
        final String customTypeId = UUID.randomUUID().toString();
        final String customTypeKey = "customTypeKey";
        final Type mockCustomType = mock(Type.class);
        when(mockCustomType.getId()).thenReturn(customTypeId);
        when(mockCustomType.getKey()).thenReturn(customTypeKey);


        final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Reference.ofResourceTypeIdAndObj(UUID.randomUUID().toString(),
                mockCustomType);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);
            mockInventoryEntries.add(mockInventoryEntry);
        }

        for (final InventoryEntry inventoryEntry : mockInventoryEntries) {
            assertThat(inventoryEntry.getCustom().getType().getId()).isEqualTo(customTypeId);
        }

        final List<InventoryEntryDraft> referenceReplacedDrafts =
            InventoryReferenceReplacementUtils.replaceInventoriesReferenceIdsWithKeys(mockInventoryEntries);

        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeKey);
        }
    }

    @Test
    public void
    replaceInventoriesReferenceIdsWithKeys_WithNonExpandedReferences_ShouldReturnReferencesWithoutReplacedKeys() {
        final String customTypeId = UUID.randomUUID().toString();
        final List<InventoryEntry> mockInventoryEntries = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final InventoryEntry mockInventoryEntry = mock(InventoryEntry.class);
            final CustomFields mockCustomFields = mock(CustomFields.class);
            final Reference<Type> typeReference = Type.referenceOfId(customTypeId);
            when(mockCustomFields.getType()).thenReturn(typeReference);
            when(mockInventoryEntry.getCustom()).thenReturn(mockCustomFields);
            mockInventoryEntries.add(mockInventoryEntry);
        }

        for (final InventoryEntry inventoryEntry : mockInventoryEntries) {
            assertThat(inventoryEntry.getCustom().getType().getId()).isEqualTo(customTypeId);
        }

        final List<InventoryEntryDraft> referenceReplacedDrafts =
            InventoryReferenceReplacementUtils.replaceInventoriesReferenceIdsWithKeys(mockInventoryEntries);

        for (InventoryEntryDraft referenceReplacedDraft : referenceReplacedDrafts) {
            assertThat(referenceReplacedDraft.getCustom().getType().getId()).isEqualTo(customTypeId);
        }
    }
}
