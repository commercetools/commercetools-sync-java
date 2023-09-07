package com.commercetools.sync.inventories;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.inventory.InventoryEntry;
import com.commercetools.api.models.inventory.InventoryEntryDraft;
import com.commercetools.api.models.inventory.InventoryEntryUpdateAction;
import com.commercetools.sync.commons.BaseSyncOptionsBuilder;
import javax.annotation.Nonnull;

/** Builder for creation of {@link com.commercetools.sync.inventories.InventorySyncOptions}. */
public final class InventorySyncOptionsBuilder
    extends BaseSyncOptionsBuilder<
        InventorySyncOptionsBuilder,
        InventorySyncOptions,
        InventoryEntry,
        InventoryEntryDraft,
        InventoryEntryUpdateAction> {
  static final int BATCH_SIZE_DEFAULT = 150;
  static final boolean ENSURE_CHANNELS_DEFAULT = false;
  private boolean ensureChannels = ENSURE_CHANNELS_DEFAULT;

  private InventorySyncOptionsBuilder(@Nonnull final ProjectApiRoot ctpClient) {
    this.ctpClient = ctpClient;
  }

  /**
   * Creates a new instance of {@link InventorySyncOptionsBuilder} given a {@link
   * com.commercetools.api.client.ProjectApiRoot} responsible for interaction with the target CTP
   * project, with the default batch size ({@code BATCH_SIZE_DEFAULT} = 150).
   *
   * @param ctpClient {@link ProjectApiRoot} responsible for interaction with the target CTP
   *     project.
   * @return new instance of {@link InventorySyncOptionsBuilder}
   */
  public static InventorySyncOptionsBuilder of(@Nonnull final ProjectApiRoot ctpClient) {
    return new InventorySyncOptionsBuilder(ctpClient).batchSize(BATCH_SIZE_DEFAULT);
  }

  /**
   * Set option that indicates whether sync process should create a supply channel of given key when
   * it doesn't exist in a target project yet. If set to {@code true} sync process would try to
   * create new supply channel of given key, otherwise the sync process would log an error and fail
   * to process the draft with the given supply channel key.
   *
   * <p>This property is {@link InventorySyncOptionsBuilder#ENSURE_CHANNELS_DEFAULT} by default.
   *
   * @param ensureChannels boolean that indicates whether sync process should create supply channel
   *     of given key when it doesn't exist in a target project yet
   * @return {@code this} instance of {@link InventorySyncOptionsBuilder}
   */
  public InventorySyncOptionsBuilder ensureChannels(final boolean ensureChannels) {
    this.ensureChannels = ensureChannels;
    return this;
  }

  /**
   * Returns new instance of {@link com.commercetools.sync.inventories.InventorySyncOptions},
   * enriched with all attributes provided to {@code this} builder.
   *
   * @return new instance of {@link com.commercetools.sync.inventories.InventorySyncOptions}
   */
  @Override
  public InventorySyncOptions build() {
    return new InventorySyncOptions(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        ensureChannels,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }

  /**
   * Returns {@code this} instance of {@link InventorySyncOptionsBuilder}.
   *
   * <p><strong>Inherited doc:</strong><br>
   * {@inheritDoc}
   */
  @Override
  protected InventorySyncOptionsBuilder getThis() {
    return this;
  }
}
