package com.commercetools.sync.sdk2.products;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.ResourceUpdateAction;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import io.sphere.sdk.products.ProductDraft;
import io.sphere.sdk.products.ProductProjection;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// TODO: While implementing this class, ResourceUpdateAction should change to ProductUpdateAction
public final class ProductSyncOptions
    extends BaseSyncOptions<ProductProjection, ProductDraft, ResourceUpdateAction> {

  protected ProductSyncOptions(
      @Nonnull ProjectApiRoot ctpClient,
      @Nullable
          QuadConsumer<
                  SyncException,
                  Optional<ProductDraft>,
                  Optional<ProductProjection>,
                  List<ResourceUpdateAction>>
              errorCallback,
      @Nullable
          TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
              warningCallback,
      int batchSize,
      @Nullable
          TriFunction<
                  List<ResourceUpdateAction>,
                  ProductDraft,
                  ProductProjection,
                  List<ResourceUpdateAction>>
              beforeUpdateCallback,
      @Nullable Function<ProductDraft, ProductDraft> beforeCreateCallback,
      long cacheSize) {
    super(
        ctpClient,
        errorCallback,
        warningCallback,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
