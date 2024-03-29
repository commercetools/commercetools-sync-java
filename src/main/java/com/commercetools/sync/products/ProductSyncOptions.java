package com.commercetools.sync.products;

import static java.util.Optional.ofNullable;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductDraft;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductUpdateAction;
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

public final class ProductSyncOptions
    extends BaseSyncOptions<ProductProjection, ProductDraft, ProductUpdateAction> {
  private final SyncFilter
      syncFilter; // which attributes to calculate update actions to black list or white list
  private final boolean ensurePriceChannels;

  ProductSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<ProductDraft>,
                  Optional<ProductProjection>,
                  List<ProductUpdateAction>>
              errorCallBack,
      @Nullable
          final TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
              warningCallBack,
      final int batchSize,
      @Nullable final SyncFilter syncFilter,
      @Nullable
          final TriFunction<
                  List<ProductUpdateAction>,
                  ProductDraft,
                  ProductProjection,
                  List<ProductUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<ProductDraft, ProductDraft> beforeCreateCallback,
      final long cacheSize,
      boolean ensurePriceChannels) {
    super(
        ctpClient,
        errorCallBack,
        warningCallBack,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
    this.syncFilter = ofNullable(syncFilter).orElseGet(SyncFilter::of);
    this.ensurePriceChannels = ensurePriceChannels;
  }

  /**
   * Returns the {@link SyncFilter} set to {@code this} {@link ProductSyncOptions}. It represents
   * either a blacklist or a whitelist for filtering certain update action groups.
   *
   * @return the {@link SyncFilter} set to {@code this} {@link ProductSyncOptions}.
   */
  @Nonnull
  public SyncFilter getSyncFilter() {
    return syncFilter;
  }

  /**
   * @return option that indicates whether the sync process should create price channel of the given
   *     key when it doesn't exist in a target project yet.
   */
  public boolean shouldEnsurePriceChannels() {
    return ensurePriceChannels;
  }
}
