package com.commercetools.sync.taxcategories;

import com.commercetools.sync.commons.BaseSyncOptions;
import com.commercetools.sync.commons.exceptions.SyncException;
import com.commercetools.sync.commons.utils.QuadConsumer;
import com.commercetools.sync.commons.utils.TriConsumer;
import com.commercetools.sync.commons.utils.TriFunction;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.taxcategories.TaxCategory;
import io.sphere.sdk.taxcategories.TaxCategoryDraft;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TaxCategorySyncOptions
    extends BaseSyncOptions<TaxCategory, TaxCategoryDraft, TaxCategory> {

  TaxCategorySyncOptions(
      @Nonnull final SphereClient ctpClient,
      @Nullable
          final QuadConsumer<
                  SyncException,
                  Optional<TaxCategoryDraft>,
                  Optional<TaxCategory>,
                  List<UpdateAction<TaxCategory>>>
              errorCallBack,
      @Nullable
          final TriConsumer<SyncException, Optional<TaxCategoryDraft>, Optional<TaxCategory>>
              warningCallBack,
      final int batchSize,
      @Nullable
          final TriFunction<
                  List<UpdateAction<TaxCategory>>,
                  TaxCategoryDraft,
                  TaxCategory,
                  List<UpdateAction<TaxCategory>>>
              beforeUpdateCallback,
      @Nullable final Function<TaxCategoryDraft, TaxCategoryDraft> beforeCreateCallback,
      final long cacheSize) {
    super(
        ctpClient,
        errorCallBack,
        warningCallBack,
        batchSize,
        beforeUpdateCallback,
        beforeCreateCallback,
        cacheSize);
  }
}
