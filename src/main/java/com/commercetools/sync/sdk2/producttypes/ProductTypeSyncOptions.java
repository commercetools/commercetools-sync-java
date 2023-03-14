package com.commercetools.sync.sdk2.producttypes;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.ProductType;
import com.commercetools.api.models.product_type.ProductTypeDraft;
import com.commercetools.api.models.product_type.ProductTypeUpdateAction;
import com.commercetools.sync.sdk2.commons.BaseSyncOptions;
import com.commercetools.sync.sdk2.commons.exceptions.SyncException;
import com.commercetools.sync.sdk2.commons.utils.QuadConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriConsumer;
import com.commercetools.sync.sdk2.commons.utils.TriFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ProductTypeSyncOptions
    extends BaseSyncOptions<ProductType, ProductTypeDraft, ProductTypeUpdateAction> {
  ProductTypeSyncOptions(
      @Nonnull final ProjectApiRoot ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<ProductTypeDraft>,
                  Optional<ProductType>,
                  List<ProductTypeUpdateAction>>
              errorCallback,
      @Nullable
          final TriConsumer<SyncException, Optional<ProductTypeDraft>, Optional<ProductType>>
              warningCallback,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<ProductTypeUpdateAction>,
                  ProductTypeDraft,
                  ProductType,
                  List<ProductTypeUpdateAction>>
              beforeUpdateCallback,
      @Nullable final Function<ProductTypeDraft, ProductTypeDraft> beforeCreateCallback,
      final long cacheSize) {

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
