package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.types.CustomFields;
import io.sphere.sdk.types.CustomFieldsDraft;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static java.util.stream.Collectors.toList;

//TODO delete this when SDK release their transformer.
/**
 * This class provides static utility methods for converting objects of {@link InventoryEntry}
 * into {@link InventoryEntryDraft}.
 */
public final class InventoryDraftTransformerUtils {

    private InventoryDraftTransformerUtils() {
        throw new AssertionError();
    }

    /**
     * Returns new {@link InventoryEntryDraft} containing same data as {@code inventoryEntry}, including
     * {@code supplyChannel} and {@code customFields} (which will be converted to {@link CustomFieldsDraft}).
     *
     * @param inventoryEntry {@link InventoryEntry} from which draft will be created
     * @return {@link InventoryEntryDraft} created for passed {@code inventoryEntry}
     */
    public static InventoryEntryDraft transformToDraft(@Nonnull final InventoryEntry inventoryEntry) {
        return InventoryEntryDraftBuilder
                .of(inventoryEntry.getSku(),
                    inventoryEntry.getQuantityOnStock(),
                    inventoryEntry.getExpectedDelivery(),
                    inventoryEntry.getRestockableInDays(),
                    inventoryEntry.getSupplyChannel())
                .custom(getCustomFieldsDraft(inventoryEntry.getCustom()))
                .build();
    }

    /**
     * Returns new {@link List} of {@link InventoryEntryDraft} created from {@code inventoryEntries}.
     * Returned list would have same size as {@code inventoryEntries} but the order of elements may differ.
     * @see InventoryDraftTransformerUtils#transformToDraft
     *
     * @param inventoryEntries list of {@link InventoryEntry} from which drafts will be created
     * @return {@link List} of {@link InventoryEntryDraft} created from {@code inventoryEntries}
     */
    public static List<InventoryEntryDraft> transformToDrafts(@Nonnull final List<InventoryEntry> inventoryEntries) {
        return inventoryEntries.stream()
                .map(InventoryDraftTransformerUtils::transformToDraft)
                .collect(toList());
    }

    /**
     * Returns new {@link CustomFieldsDraft} containing same data as {@code customFields}.
     *
     * @param customFields {@link CustomFields} from which draft will be created
     * @return {@link CustomFieldsDraft} created from {@code customFields} or {@code null} for {@code null} parameter
     */
    private static CustomFieldsDraft getCustomFieldsDraft(@Nullable final CustomFields customFields) {
        if (customFields != null) {
            final Type type = customFields.getType().getObj();
            if (type != null && type.getKey() != null) {
                return CustomFieldsDraft.ofTypeKeyAndJson(type.getKey(), customFields.getFieldsJsonMap());
            } else {
                return CustomFieldsDraft.ofTypeIdAndJson(customFields.getType().getId(), customFields
                    .getFieldsJsonMap());
            }
        } else {
            return null;
        }
    }
}
