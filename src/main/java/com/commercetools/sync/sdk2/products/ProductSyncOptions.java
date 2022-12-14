package com.commercetools.sync.sdk2.products;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductUpdateAction;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ProductSyncOptions
    extends BaseSyncOptions<ProductProjection, ProductDraft, ProductUpdateAction> {

  protected ProductSyncOptions(
      @NotNull ProjectApiRoot ctpClient,
      @Nullable
          QuadConsumer<
                  SyncException,
                  Optional<ProductDraft>,
                  Optional<ProductProjection>,
                  List<ProductUpdateAction>>
              errorCallback,
      @Nullable
          TriConsumer<SyncException, Optional<ProductDraft>, Optional<ProductProjection>>
              warningCallback,
      int batchSize,
      @Nullable
          TriFunction<
                  List<ProductUpdateAction>,
                  ProductDraft,
                  ProductProjection,
                  List<ProductUpdateAction>>
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
