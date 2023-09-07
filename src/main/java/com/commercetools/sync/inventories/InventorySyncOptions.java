package com.commercetools.sync.inventories;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryUpdateAction;
import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class InventorySyncOptions
    extends BaseSyncOptions<InventoryEntry, InventoryEntryDraft, InventoryEntryUpdateAction> {
  private final boolean ensureChannels;

  InventorySyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<InventoryEntryDraft>,
                  Optional<InventoryEntry>,
                  List<InventoryEntryUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<InventoryEntryDraft>, Optional<InventoryEntry>>
              warningCallback,
      final int batchSize,
      boolean ensureChannels,
      @Nullable
          final TriFunction<
                  List<InventoryEntryUpdateAction>,
                  InventoryEntryDraft,
                  InventoryEntry,
                  List<InventoryEntryUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<InventoryEntryDraft, InventoryEntryDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
    this.ensureChannels = ensureChannels;
  }

  /**
   * @return option that indicates whether the sync process should create a supply channel of the
   *     given key when it doesn't exist in a target system yet.
   */
  public boolean shouldEnsureChannels() {
    return ensureChannels;
  }
}
