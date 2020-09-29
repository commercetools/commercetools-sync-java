package com.commercetools.sync.inventories.helpers;

import com.commercetools.sync.commons.helpers.BaseBatchValidator;
import com.commercetools.sync.inventories.InventorySyncOptions;
import io.sphere.sdk.inventory.InventoryEntryDraft;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class InventoryBatchValidator
    extends BaseBatchValidator<InventoryEntryDraft, InventorySyncOptions, InventorySyncStatistics> {

    static final String INVENTORY_DRAFT_SKU_NOT_SET = "InventoryEntryDraft doesn't have a SKU. "
        + "Please make sure all inventory entry drafts have SKUs.";
    static final String INVENTORY_DRAFT_IS_NULL = "InventoryEntryDraft is null.";

    public InventoryBatchValidator(
        @Nonnull final InventorySyncOptions syncOptions,
        @Nonnull final InventorySyncStatistics syncStatistics) {

        super(syncOptions, syncStatistics);
    }

    /**
     * Given the {@link List}&lt;{@link InventoryEntryDraft}&gt; of drafts this method attempts to validate
     * drafts and collect referenced keys from the draft
     * and return an {@link ImmutablePair}&lt;{@link Set}&lt;{@link InventoryEntryDraft}&gt;,{@link ReferencedKeys}&gt;
     * which contains the {@link Set} of valid drafts and referenced keys within a wrapper.
     *
     * <p>A valid inventory draft is one which satisfies the following conditions:
     * <ol>
     * <li>It is not null</li>
     * <li>It has a sku which is not blank (null/empty)</li>
     * </ol>
     *
     * @param inventoryDrafts the inventory drafts to validate and collect referenced keys.
     * @return {@link ImmutablePair}&lt;{@link Set}&lt;{@link InventoryEntryDraft}&gt;,{@link ReferencedKeys}&gt;
     *      which contains the {@link Set} of valid drafts and referenced keys within a wrapper.
     */
    @Override
    public ImmutablePair<Set<InventoryEntryDraft>, ReferencedKeys> validateAndCollectReferencedKeys(
        @Nonnull final List<InventoryEntryDraft> inventoryDrafts) {
        final ReferencedKeys referencedKeys = new ReferencedKeys();
        final Set<InventoryEntryDraft> validDrafts = inventoryDrafts
            .stream()
            .filter(this::isValidInventoryEntryDraft)
            .peek(inventoryDraft -> collectReferencedKeys(referencedKeys, inventoryDraft))
            .collect(Collectors.toSet());

        return ImmutablePair.of(validDrafts, referencedKeys);
    }

    private boolean isValidInventoryEntryDraft(@Nullable final InventoryEntryDraft inventoryDraft) {
        if (inventoryDraft == null) {
            handleError(INVENTORY_DRAFT_IS_NULL);
        } else if (isBlank(inventoryDraft.getSku())) {
            handleError(INVENTORY_DRAFT_SKU_NOT_SET);
        } else {
            return true;
        }

        return false;
    }

    private void collectReferencedKeys(
        @Nonnull final ReferencedKeys referencedKeys,
        @Nonnull final InventoryEntryDraft inventoryDraft) {

        collectReferencedKeyFromResourceIdentifier(inventoryDraft.getSupplyChannel(),
            referencedKeys.channelKeys::add);
        collectReferencedKeyFromCustomFieldsDraft(inventoryDraft.getCustom(),
            referencedKeys.typeKeys::add);
    }

    public static class ReferencedKeys {
        private final Set<String> channelKeys = new HashSet<>();
        private final Set<String> typeKeys = new HashSet<>();

        public Set<String> getTypeKeys() {
            return typeKeys;
        }

        public Set<String> getChannelKeys() {
            return channelKeys;
        }
    }
}
