package com.commercetools.sync.inventories.utils;

import io.sphere.sdk.channels.Channel;
import io.sphere.sdk.inventory.InventoryEntry;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import io.sphere.sdk.inventory.InventoryEntryDraftBuilder;
import io.sphere.sdk.models.Reference;
import io.sphere.sdk.models.ResourceIdentifier;
import io.sphere.sdk.types.Type;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.commercetools.sync.commons.utils.CustomTypeReferenceResolutionUtils.mapToCustomFieldsDraft;
import static java.util.stream.Collectors.toList;

/**
 * Util class which provides utilities that can be used when syncing resources from a source commercetools project
 * to a target one.
 */
public final class InventoryReferenceResolutionUtils {

    /**
     * Returns an {@link List}&lt;{@link InventoryEntryDraft}&gt; consisting of the results of applying the
     * mapping from {@link InventoryEntry} to {@link InventoryEntryDraft} with considering reference resolution.
     *
     * <table summary="Mapping of Reference fields for the reference resolution">
     *   <thead>
     *     <tr>
     *       <th>Reference field</th>
     *       <th>from</th>
     *       <th>to</th>
     *     </tr>
     *   </thead>
     *   <tbody>
     *     <tr>
     *       <td>supplyChannel</td>
     *       <td>{@link Reference}&lt;{@link Channel}&gt;</td>
     *       <td>{@link ResourceIdentifier}&lt;{@link Channel}&gt;</td>
     *     </tr>
     *     <tr>
     *        <td>custom.type</td>
     *        <td>{@link Reference}&lt;{@link Type}&gt;</td>
     *        <td>{@link ResourceIdentifier}&lt;{@link Type}&gt;</td>
     *     </tr>
     *   </tbody>
     * </table>
     *
     * <p><b>Note:</b> The {@link Channel} and {@link Type} references should be expanded with a key.
     * Any reference that is not expanded will have its id in place and not replaced by the key will be
     * considered as existing resources on the target commercetools project and
     * the library will issues an update/create API request without reference resolution.
     *
     * @param inventoryEntries the inventory entries with expanded references.
     * @return a {@link List} of {@link InventoryEntryDraft} built from the
     *         supplied {@link List} of {@link InventoryEntry}.
     */
    @Nonnull
    public static List<InventoryEntryDraft> mapToInventoryEntryDrafts(
        @Nonnull final List<InventoryEntry> inventoryEntries) {

        return inventoryEntries
            .stream()
            .map(InventoryReferenceResolutionUtils::mapToInventoryEntryDraft)
            .collect(toList());
    }

    @Nonnull
    private static InventoryEntryDraft mapToInventoryEntryDraft(@Nonnull final InventoryEntry inventoryEntry) {
        return InventoryEntryDraftBuilder
            .of(inventoryEntry)
            .custom(mapToCustomFieldsDraft(inventoryEntry))
            .supplyChannel(mapToChannelResourceIdentifier(inventoryEntry))
            .build();
    }

    @Nullable
    private static ResourceIdentifier<Channel> mapToChannelResourceIdentifier(
        @Nonnull final InventoryEntry inventoryEntry) {
        final Reference<Channel> supplyChannelReference = inventoryEntry.getSupplyChannel();
        if (supplyChannelReference != null) {
            if (supplyChannelReference.getObj() != null) {
                return ResourceIdentifier.ofKey(supplyChannelReference.getObj().getKey());
            }
            return ResourceIdentifier.ofId(supplyChannelReference.getId());
        }
        return null;
    }


    private InventoryReferenceResolutionUtils() {
    }
}
